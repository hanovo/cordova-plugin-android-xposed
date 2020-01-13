package com.skynet.xposed.hookers.wechat;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.WindowManager;

import com.skynet.xposed.hookers.BaseAppHooker;
import com.skynet.xposed.hookers.HookableApps;
import com.skynet.xposed.utils.PayHelperUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 微信钩子。
 */
public class WechatHooker extends BaseAppHooker {
    private static String TAG = WechatHooker.class.getSimpleName();

    private static WechatHooker inst = null;

    /**
     * 构造函数。
     */
    private WechatHooker() {
    }

    /**
     * 单例。
     */
    public static WechatHooker inst() {
        if (inst == null) {
            inst = new WechatHooker();
        }

        return inst;
    }

    /**
     * App Hook。
     */
    public void hook(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            boolean isHooked = HookableApps.inst().isHooked(HookableApps.PackageWechat);
            if (isHooked) return;

            HookableApps.inst().statHookedAppListening(HookableApps.PackageWechat);

            XposedBridge.log(TAG + ": 即将开始Hook支微信...");

            mClassLoader = lpparam.classLoader;

            // Hook 应用本身
            this.hookApplication();

            // Hook 二维码窗口
            this.hookQRWindows();

            // Hook 账单
            this.hookBill();

            XposedBridge.log(TAG + ": 微信Hook成功。");
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": 微信Hook失败。" + e.getMessage());
        }
    }

    /**
     * Hook 微信应用。
     */
    public void hookApplication() {
        try {
            Class<?> ContextClass = XposedHelpers.findClass("android.content.ContextWrapper", mClassLoader);

            XposedHelpers.findAndHookMethod(ContextClass, "getApplicationContext", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    super.afterHookedMethod(param);
                    if (mAppContext != null) return;

                    mAppContext = (Context) param.getResult();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": 获取微信上下文出错。" + t.getMessage());
        }
    }

    /**
     * Hook 二维码创建窗口，目的是为了创建生成二维码
     */
    private void hookQRWindows() {
        Class<?> clazz = XposedHelpers.findClass("com.tencent.mm.plugin.collect.ui.CollectCreateQRCodeUI", mClassLoader);
        XposedBridge.hookAllMethods(clazz, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XposedBridge.log("Hook到微信窗口");

                ((Activity) param.thisObject).getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            }
        });

        XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.collect.ui.CollectCreateQRCodeUI", mClassLoader, "initView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("Hook微信开始......");

                Intent intent = ((Activity) param.thisObject).getIntent();
                String mark = intent.getStringExtra("mark");
                String money = intent.getStringExtra("money");

                Class<?> bs = XposedHelpers.findClass("com.tencent.mm.plugin.collect.b.s", mClassLoader);
                Object obj = XposedHelpers.newInstance(bs, Double.valueOf(money), "1", mark);
                XposedHelpers.callMethod(param.thisObject, "a", obj, true, true);
            }
        });
    }

    /**
     * Hook 微信账单。
     */
    private void hookBill() {
        XposedHelpers.findAndHookMethod("com.tencent.wcdb.database.SQLiteDatabase", mClassLoader, "insert", String.class, String.class, ContentValues.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    ContentValues contentValues = (ContentValues) param.args[2];
                    String tableName = (String) param.args[0];
                    if (TextUtils.isEmpty(tableName) || !tableName.equals("message")) return;

                    Integer type = contentValues.getAsInteger("type");
                    if (null == type) return;

                    // XposedBridge.log("\n\n\n遍历content里的信息：");
                    // for(Map.Entry<String, Object> item : contentValues.valueSet())
                    // {
                    //    XposedBridge.log(item.getKey() + " , " + item.getValue().toString());
                    // }
                    // XposedBridge.log("遍历content里的信息完成\n\n\n");

                    if (type == 318767153) {
                        String contentStr = contentValues.getAsString("content");
                        JSONObject msg = XML.toJSONObject(contentStr);
                        XposedBridge.log("收款信息(json)：" + msg);

                        JSONObject mmreader = msg.getJSONObject("msg").getJSONObject("appmsg").getJSONObject("mmreader");
                        //获取时间
                        long time = mmreader.getJSONObject("template_header").getLong("pub_time");
                        Date currentTime = new Date(time);
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        String dateString = formatter.format(currentTime);

                        // 获取收款明细
                        JSONObject billDetail = mmreader.getJSONObject("template_detail").getJSONObject("line_content");

                        // 获取收款标题、金额等
                        JSONObject topLine = billDetail.getJSONObject("topline");

                        // 收款标题
                        String topLineKey = topLine.getJSONObject("key").getString("word");

                        // 收款金额
                        String topLineValue = topLine.getJSONObject("value").getString("word");
                        float money = Float.parseFloat(topLineValue.replace("￥", ""));

                        // 获取汇总、备注等信息
                        JSONArray line = billDetail.getJSONObject("lines").getJSONArray("line");

                        // 获取汇总
                        JSONObject line0Detail = line.getJSONObject(0);
                        String line0Title = line0Detail.getJSONObject("key").getString("word");
                        String line0Msg = line0Detail.getJSONObject("value").getString("word");

                        // 获取备注
                        JSONObject line1Detail = line.getJSONObject(1);
                        String line1Title = line1Detail.getJSONObject("key").getString("word");
                        String line1Msg = line1Detail.getJSONObject("value").getString("word");

                        XposedBridge.log("\n\n\n获取到时间：" + dateString);
                        XposedBridge.log(topLineKey + money);
                        XposedBridge.log(line0Title + " " + line0Msg);
                        XposedBridge.log(line1Title + " " + line1Msg);

                        XposedBridge.log("开始通知客户端");
                        XposedBridge.log("--------------------");
                        String data = "<time>" + dateString + "</time>" +
                                "<topline>" + "<key>" + topLineKey + "</key>" + "<value>" + money + "</value>" + "</topline>" +
                                "<line>" + "<key>" + line0Title + "</key>" + "<value>" + line0Msg + "</value>" + "</line>" +
                                "<line>" + "<key>" + line1Title + "</key>" + "<value>" + line1Msg + "</value>" + "</line>";

                        //把信息广播出去
                        // Intent intent = new Intent("com.example.a32960.moudletest");
                        // intent.putExtra("xmlData", data);
                        // mAppContext.sendBroadcast(intent);
                    }
                } catch (Exception e) {
                    XposedBridge.log("获取信息出错： " + e.getMessage());
                }
            }
        });
    }

    /**
     * 获取微信登录用户ID。
     */
    public String getWechatLoginId(Context context) {
        String loginId = "";

        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.tencent.mm_preferences", 0);
            loginId = sharedPreferences.getString("login_user_name", "");
        } catch (Exception e) {
            PayHelperUtils.sendMsg(context, e.getMessage());
        }

        return loginId;
    }

    /**
     * 获取微信余额。
     */
    public double getWechatBalance(ClassLoader classLoader) {
        double balance = 0.0;

        Class<?> p = this.findClass("com.tencent.mm.plugin.wallet.a.p");
        XposedHelpers.callStaticMethod(p, "bNp");
        Object ag = XposedHelpers.callStaticMethod(p, "bNq");
        Object paw = XposedHelpers.getObjectField(ag, "paw");

        if (paw != null) {
            balance = (Double) XposedHelpers.getObjectField(paw, "plV");
        }

        return balance;
    }
}
