package com.skynet.xposed.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Base64;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;
import com.skynet.xposed.hookers.alipay.AlipayDataCache;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XposedBridge;

public class PayHelperUtils {
  //渠道
  public static String VERSION2 = "v3_673";

  public static String VERSIONV1 = "3";
  public static String VERSIONV2 = VERSION2 + "1.0.3";
  public static String VERSIONV0 = "v20181101";
  public static String VERSIONVKEY = "8D2swf55wk45895y8QnajpYH7h4Q2BK";

  public static boolean isFirst = true;

  /**
   * 将图片转换成Base64编码的字符串
   *
   * @param path
   * @return base64编码的字符串
   */
  public static String imageToBase64(String path) {
    if (TextUtils.isEmpty(path)) return null;

    InputStream is = null;
    String result = null;

    byte[] data;
    try {
      is = new FileInputStream(path);

      // 创建一个字符流大小的数组。
      data = new byte[is.available()];

      // 写入数组
      is.read(data);

      // 用默认的编码格式进行编码
      result = Base64.encodeToString(data, Base64.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (null != is) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

    }

    result = "\"data:image/gif;base64," + result + "\"";
    return result;
  }

  /**
   * 发送启动App消息，以启动对应的App。
   */
  public static void sendLaunchAppMsg(String money, String mark, String type, Context context) {
    String action = null;
    switch (type) {
      case "alipay":
        action = IntentActions.AlipayLaunchCollectUp;
        break;
      case "wechat":
        action = IntentActions.WechatLaunch;
        break;
      case "qq":
        action = IntentActions.QQLaunch;
        break;
      case "unionpay":
        action = IntentActions.UnionPayLaunch;
        break;
    }

    Intent broadCastIntent = new Intent();
    broadCastIntent.setAction(action);
    broadCastIntent.putExtra("mark", mark);
    broadCastIntent.putExtra("money", money);
    context.sendBroadcast(broadCastIntent);
  }

  /**
   * 将时间戳转换为时间
   */
  public static String timestampToDate(String s) {
    String res;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    long lt = new Long(s);
    Date date = new Date(lt * 1000);
    res = simpleDateFormat.format(date);
    return res;
  }

  /**
   * 启动银联获取二维码服务
   */
  public static void startUnionPayService(Context context) {
    Intent unionIntent = new Intent();
    unionIntent.setAction("com.colin.union.start");
    context.sendBroadcast(unionIntent);
  }

  /**
   * 通知。
   */
  public static void notify(final Context context,
                            String type,
                            final String no,
                            String money,
                            String mark,
                            String dt) {
    String _notifyapi = null;
    String url = AbSharedUtil.getString(context, "callbackUrl");
    _notifyapi = url;

    // 截取Mark获得通知地址
    if (mark.contains("brt")) {
      _notifyapi = url.replaceAll("[a-z]+\\d+", mark.substring(0, 6));
      mark = mark.substring(6, mark.length() - 1);
    }

    final String callbackUrl = _notifyapi;
    String signKey = AbSharedUtil.getString(context, "signToken");
    sendMsg(context, "订单" + no + "重试发送异步通知(" + VERSIONV2 + ")...");

    if (TextUtils.isEmpty(callbackUrl) || TextUtils.isEmpty(signKey)) {
      sendMsg(context, "发送异步通知(" + VERSIONV2 + ")异常，异步通知地址或密钥为空");
      update(no, "异步通知地址(" + VERSIONV2 + ")或密钥为空");
      return;
    }

    String account = "";
    String balance = AbSharedUtil.getString(context, type + "balance");
    if (type.equals("alipay")) {
      account = AbSharedUtil.getString(context, "alipay");
    } else if (type.equals("wechat")) {
      account = AbSharedUtil.getString(context, "wechat");
    } else if (type.equals("qq")) {
      account = AbSharedUtil.getString(context, "qq");
    }

    HttpUtils httpUtils = new HttpUtils(30000);

    String sign = MD5.digest(dt + mark + money + no + type + signKey + AbSharedUtil.getString(context, "userids") + VERSIONV0);
    RequestParams params = new RequestParams();
    params.addBodyParameter("type", type);
    params.addBodyParameter("no", no);
    params.addBodyParameter("version", VERSIONV0);
    params.addBodyParameter("userids", AbSharedUtil.getString(context, "userids"));


    params.addBodyParameter("money", money);
    params.addBodyParameter("mark", mark);
    params.addBodyParameter("dt", dt);
    params.addBodyParameter("balance", balance);
    if (!TextUtils.isEmpty(account)) {
      params.addBodyParameter("account", account);
    }
    if (type.equals("alipay")) {
      params.addBodyParameter("is_customization", "1");

    }
    params.addBodyParameter("sign", sign);

    sendMsg(context, "发送" + dt + mark + money + no + type + signKey + AbSharedUtil.getString(context, "userids") + VERSIONV0 + "异步通知(" + callbackUrl + ")，密钥是" + signKey);
    httpUtils.send(HttpMethod.POST, callbackUrl, params, new RequestCallBack<String>() {
      @Override
      public void onFailure(HttpException arg0, String arg1) {
        sendMsg(context, "发送异步通知(" + callbackUrl + ")异常，服务器异常" + arg1);
        update(no, arg1);
      }

      @Override
      public void onSuccess(ResponseInfo<String> arg0) {
        String result = arg0.result;
        if (result.contains("success")) {
          sendMsg(context, "发送异步通知(" + callbackUrl + ")成功，服务器返回" + result);
        } else {
          sendMsg(context, "发送异步通知(" + callbackUrl + ")失败，服务器返回" + result);
        }
        update(no, result);
      }
    });
  }

  private static void update(String no, String result) {
    // AlipayDataCache dbManager = new AlipayDataCache(App.getInstance().getApplicationContext());
    // dbManager.updateOrder(no, result);
  }

  /**
   * 发送交易信息。
   */
  public static void sendTradeInfo(Context context) {
    Intent broadCastIntent = new Intent();
    broadCastIntent.setAction(IntentActions.GetTradeInfo);
    context.sendBroadcast(broadCastIntent);
  }

  /**
   * 获取交易信息。
   */
  public static void getTradeInfo(final Context context, final String cookie) {
    sendMsg(context, "有新的商家服务订单进来！！！");
    String url = "https://mbillexprod.alipay.com/enterprise/walletTradeList.json?lastTradeNo=&lastDate=&pageSize=1&shopId=&_input_charset=utf-8&ctoken==&_ksTS=&_callback=&t=" + System.currentTimeMillis();
    HttpUtils httpUtils = new HttpUtils(30000);
    httpUtils.configResponseTextCharset("GBK");
    RequestParams params = new RequestParams();
    params.addHeader("Cookie", cookie);
    params.addHeader("Referer", "https://render.alipay.com/p/z/merchant-mgnt/simple-order.html");
    params.addHeader("User-Agent", "Mozilla/5.0 (Linux; U; Android 7.1.1; zh-cn; Redmi Note 3 Build/LRX22G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/1.0.0.100 U3/0.8.0 Mobile Safari/534.30 Nebula AlipayDefined(nt:WIFI,ws:360|640|3.0) AliApp(AP/10.1.22.835) AlipayClient/10.1.22.835 Language/zh-Hans useStatusBar/true");
    httpUtils.send(HttpMethod.GET, url, params, new RequestCallBack<String>() {
      @Override
      public void onFailure(HttpException arg0, String arg1) {
        sendMsg(context, "请求支付宝API失败，出现掉单，5秒后启动补单");
        sendTradeInfo(context);
      }

      @Override
      public void onSuccess(ResponseInfo<String> arg0) {
        String result = arg0.result.replace("/**/(", "").replace("})", "}");
        try {
          JSONObject jsonObject = new JSONObject(result);
          if (jsonObject.has("status")) {
            String status = jsonObject.getString("status");
            if (!status.equals("deny")) {
              JSONObject res = jsonObject.getJSONObject("result");
              JSONArray jsonArray = res.getJSONArray("list");
              if (jsonArray != null && jsonArray.length() > 0) {
                JSONObject object = jsonArray.getJSONObject(0);
                String tradeNo = object.getString("tradeNo");
                Intent broadCastIntent = new Intent();
                broadCastIntent.putExtra("tradeno", tradeNo);
                broadCastIntent.putExtra("cookie", cookie);
                broadCastIntent.setAction(IntentActions.AppTradeNoReceived);
                context.sendBroadcast(broadCastIntent);
              }
            } else {
              sendMsg(context, "getTradeInfo=>>支付宝cookie失效，出现掉单，5秒后启动补单");
              sendTradeInfo(context);
            }
          }
        } catch (Exception e) {
          sendMsg(context, e.getMessage());
          XposedBridge.log("=====>" + arg0);
          sendMsg(context, "getTradeInfo出现异常=>>" + result);
          sendMsg(context, "出现掉单，5秒后启动补单");
          sendTradeInfo(context);
        }
      }
    });
  }

  /**
   * 获取支付宝的交易信息。
   */
  public static void getTradeInfo2(final Context context) {
    sendMsg(context, "开始补单！！！");

    final AlipayDataCache dbManager = new AlipayDataCache(context);
    final String cookie = getAlipayCookie(context);
    String url = "https://mbillexprod.alipay.com/enterprise/walletTradeList.json?lastTradeNo=&lastDate=&pageSize=50&shopId=&_input_charset=utf-8&ctoken==&_ksTS=&_callback=&t=" + System.currentTimeMillis();
    HttpUtils httpUtils = new HttpUtils(30000);
    httpUtils.configResponseTextCharset("GBK");
    RequestParams params = new RequestParams();
    params.addHeader("Cookie", cookie);
    params.addHeader("Referer", "https://render.alipay.com/p/z/merchant-mgnt/simple-order.html");
    params.addHeader("User-Agent", "Mozilla/5.0 (Linux; U; Android 7.1.1; zh-cn; Redmi Note 3 Build/LRX22G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/1.0.0.100 U3/0.8.0 Mobile Safari/534.30 Nebula AlipayDefined(nt:WIFI,ws:360|640|3.0) AliApp(AP/10.1.22.835) AlipayClient/10.1.22.835 Language/zh-Hans useStatusBar/true");
    httpUtils.send(HttpMethod.GET, url, params, new RequestCallBack<String>() {
      @Override
      public void onFailure(HttpException arg0, String arg1) {
        sendMsg(context, "服务器异常" + arg1);
      }

      @Override
      public void onSuccess(ResponseInfo<String> arg0) {
        String result = arg0.result.replace("/**/(", "").replace("})", "}");
        try {
          JSONObject jsonObject = new JSONObject(result);
          if (jsonObject.has("status")) {
            String status = jsonObject.getString("status");
            if (!status.equals("deny")) {
              JSONObject res = jsonObject.getJSONObject("result");
              JSONArray jsonArray = res.getJSONArray("list");
              if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                  JSONObject object = jsonArray.getJSONObject(i);
                  String tradeNo = object.getString("tradeNo");
                  if (!dbManager.isExistTradeNo(tradeNo)) {
                    sendMsg(context, "补单:::请求完成获取到流水号,订单未处理，发送广播：" + tradeNo);
                    Intent broadCastIntent = new Intent();
                    broadCastIntent.putExtra("tradeno", tradeNo);
                    broadCastIntent.putExtra("cookie", cookie);
                    broadCastIntent.setAction(IntentActions.AppTradeNoReceived);
                    context.sendBroadcast(broadCastIntent);
                  }
                }
              }
            } else {
              sendMsg(context, "getTradeInfo2=>>补单失败，cookie失效");
            }
          }
        } catch (Exception e) {
          sendMsg(context, "getTradeInfo异常2=>>补单失败" + arg0);
          XposedBridge.log("----arg0-----:" + arg0);
        }
      }
    });
  }

  public static String getCurrentDate() {
    long l = System.currentTimeMillis();
    Date date = new Date(l);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String d = dateFormat.format(date);
    return d;
  }

  /**
   * 向App发送一条广播，收到一条消息。
   */
  public static void sendMsg(Context context, String msg) {
    Intent broadCastIntent = new Intent();
    broadCastIntent.putExtra("msg", msg);
    broadCastIntent.setAction(IntentActions.AppMessageReceived);

    context.sendBroadcast(broadCastIntent);
  }

  /**
   * 判断App是否已安装。
   */
  public static boolean isAppInstalled(Activity activity, String name) {
    Intent intent = new Intent();
    intent.setAction(name);
    PackageManager pm = activity.getPackageManager();
    List<ResolveInfo> resolveInfos = pm.queryBroadcastReceivers(intent, 0);
    if (resolveInfos != null && !resolveInfos.isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * 判断某activity是否处于栈顶
   *
   * @return true在栈顶 false不在栈顶
   */
  public static int isActivityTop(Context context) {
    try {
      ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      List<RunningTaskInfo> infos = manager.getRunningTasks(100);
      for (RunningTaskInfo runningTaskInfo : infos) {
        if (runningTaskInfo.topActivity.getClassName().equals("cooperation.qwallet.plugin.QWalletPluginProxyActivity")) {
          return runningTaskInfo.numActivities;
        }
      }

      return 0;
    } catch (SecurityException e) {
      sendMsg(context, e.getMessage());

      return 0;
    }
  }

  /**
   * 发送当前登录用户ID。
   */
  public static void sendLoginId(Context context, String type, String loginId) {
    Intent broadCastIntent = new Intent();
    broadCastIntent.setAction(IntentActions.AppLoginIdReceived);
    broadCastIntent.putExtra("type", type);
    broadCastIntent.putExtra("loginid", loginId);
    context.sendBroadcast(broadCastIntent);
  }

  /**
   * 更新支付宝Cookie。
   */
  public static void updateAlipayCookie(Context context, String cookie) {
    AlipayDataCache dbManager = new AlipayDataCache(context);
    if (dbManager.getConfig("cookie").equals("null")) {
      dbManager.addConfig("cookie", cookie);
    } else {
      dbManager.updateConfig("cookie", cookie);
    }
  }

  /**
   * 从本地缓存中获取支付宝Cookie。
   */
  public static String getAlipayCookie(Context context) {
    AlipayDataCache dbManager = new AlipayDataCache(context);
    String cookie = dbManager.getConfig("cookie");
    return cookie;
  }

  /**
   * 获取支付宝的账单列表。获取从现在往后数一天的订单量，然后与本地数据库对比。
   * 若已存在则忽略，否则将订单数据存到本地数据库。
   */
  public static void downloadAlipayBills(final Context context) {
    try {
      Timer timer = new Timer();
      TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
          Calendar cc = Calendar.getInstance();
          int nowmi = cc.get(Calendar.MINUTE);
          int nowhour = cc.get(Calendar.HOUR_OF_DAY);
          int nowhour12 = cc.get(Calendar.HOUR);
          if (nowhour12 > 9) nowhour12 = nowhour12 - 9;

          String nowMinute = AbSharedUtil.getString(context, "nowMinute");
          int triggerTime = AbSharedUtil.getInt(context, "triggerTime") > 0 ? AbSharedUtil.getInt(context, "triggerTime") : 3;
          if (nowmi % triggerTime == 0 && nowMinute.equals("0")) {
            AbSharedUtil.putString(context, "nowMinute", "1");

            sendMsg(context, "每间隔" + triggerTime + "分钟轮询获取订单数据...");

            final AlipayDataCache dbManager = new AlipayDataCache(context);
            dbManager.saveOrUpdateConfig("time", System.currentTimeMillis() / 1000 + "");

            final String cookie = PayHelperUtils.getAlipayCookie(context);
            if (TextUtils.isEmpty(cookie) || cookie.equals("null")) return;

            long current = System.currentTimeMillis();
            long s = current - 864000000;
            String c = getCurrentDate();
            String url = "https://mbillexprod.alipay.com/enterprise/simpleTradeOrderQuery.json?beginTime=" + s
                    + "&limitTime=" + current + "&pageSize=50&pageNum=1&channelType=ALL";
            HttpUtils httpUtils = new HttpUtils(30000);
            httpUtils.configResponseTextCharset("GBK");
            RequestParams params = new RequestParams();
            params.addHeader("Cookie", cookie);
            params.addHeader("Referer", "https://render.alipay.com/p/z/merchant-mgnt/simple-order.html?beginTime=" + c
                    + "&endTime=" + c + "&fromBill=true&channelType=ALL");

            httpUtils.send(HttpMethod.GET, url, params, new RequestCallBack<String>() {
              @Override
              public void onFailure(HttpException arg0, String arg1) {
                sendMsg(context, "服务器异常" + arg1);
                sendMsg(context, "数据异常");
              }

              @Override
              public void onSuccess(ResponseInfo<String> arg0) {
                try {
                  String result = arg0.result;
                  JSONObject jsonObject = new JSONObject(result);
                  if (jsonObject.has("status")) {
                    String status = jsonObject.getString("status");

                    if (!status.equals("deny")) {
                      JSONObject res = jsonObject.getJSONObject("result");
                      JSONArray jsonArray = res.getJSONArray("list");

                      if (jsonArray != null && jsonArray.length() > 0) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                          JSONObject object = jsonArray.getJSONObject(i);
                          String tradeNo = object.getString("tradeNo");

                          if (!dbManager.isExistTradeNo(tradeNo)) {
                            if (!dbManager.isNotifyTradeNo(tradeNo)) {
                              Intent broadCastIntent = new Intent();
                              sendMsg(context, "ACTION_TRADE_NO_RECEIVED tradeNo is " + tradeNo);
                              broadCastIntent.putExtra("tradeno", tradeNo);
                              broadCastIntent.putExtra("cookie", cookie);
                              broadCastIntent.setAction(IntentActions.AppTradeNoReceived);
                              context.sendBroadcast(broadCastIntent);
                            } else {
                              sendMsg(context, "该订单已Notify过了。交易ID：" + tradeNo);
                            }
                          } else {
                            sendMsg(context, "该订单已处理过了。交易ID：" + tradeNo);
                          }
                        }

                        isFirst = false;
                      }
                    }
                  }
                } catch (Exception e) {
                  sendMsg(context, "[下载支付宝订单异常]: " + e.getMessage());
                }

                isFirst = false;
              }
            });
          } else {
            if (nowmi % -triggerTime == 0) {
              // todo:
            } else {
              AbSharedUtil.putString(context, "nowMinute", "0");
            }
          }
        }
      };

      int _triggerTime = 10;
      timer.schedule(timerTask, 0, _triggerTime * 1000);
    } catch (Exception e) {
      sendMsg(context, "检查支付宝订单出错: " + e.getMessage());
    }
  }

  /**
   * 获取当前时间，毫秒。
   */
  public static String getCurrentTimeMillis(Context context) {
    AlipayDataCache dbManager = new AlipayDataCache(context);
    return dbManager.getConfig("time");
  }

  /**
   * 发送更新用户余额消息。
   */
  public static void sendUpdateBalanceMsg(String type, String balance, Context context) {
    Intent broadCastIntent = new Intent();
    broadCastIntent.setAction(IntentActions.UpdateUserBalance);
    broadCastIntent.putExtra("type", type);
    broadCastIntent.putExtra("balance", balance);
    context.sendBroadcast(broadCastIntent);
  }

  /**
   * 获取本地存储的余额。
   */
  public static String getUserBalance(String type, Context context) {
    String balance = AbSharedUtil.getString(context, type + "balance");
    return balance;
  }
}
