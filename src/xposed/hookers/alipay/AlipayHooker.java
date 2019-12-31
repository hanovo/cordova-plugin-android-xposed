package com.skynet.xposed.hookers.alipay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import com.alibaba.fastjson.JSON;
import com.skynet.xposed.cordova.plugin.PluginIntentActions;
import com.skynet.xposed.hookers.BaseAppHooker;
import com.skynet.xposed.hookers.HookableApps;
import com.skynet.xposed.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;

/**
 * 支付宝钩子。
 */
public class AlipayHooker extends BaseAppHooker {
  private static String TAG = AlipayHooker.class.getSimpleName();

  private static AlipayHooker inst = null;
  private static List<Bundle> bundleList = new ArrayList<>();

  private Handler deleteContactHandler = null;  // 好友删除消息处理器

  // private Map<String, Object> loginUserInfo = null;

  /**
   * 构造函数。
   */
  private AlipayHooker() {
  }

  /**
   * 单例。
   */
  public static AlipayHooker inst() {
    if (inst == null) {
      inst = new AlipayHooker();
    }

    return inst;
  }

  /**
   * App Hook。
   */
  public void hook(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
    try {
      boolean isHooked = HookableApps.inst().isHooked(HookableApps.PackageAlipay);
      if (isHooked) return;

      HookableApps.inst().statHookedAppListening(HookableApps.PackageAlipay);

      XposedBridge.log(TAG + ": 即将开始Hook支付宝...");

      mClassLoader = lpparam.classLoader;

      // Hook 应用本身
      this.hookApplication();

      // Hook 启动页
      this.hookLauncherActivity(lpparam);

      // Hook TradeDao 插入方法(即到账的时候执行)
      this.hookTradeDaoInsertMessageInfo();

      // Hook 消息数据库插入方法（监听到账消息）
      // AlipayHooker.this.hookMessageBoxServiceDaoInsertMessageInfo();

      // Hook 私聊 Activity
      // AlipayHooker.this.hookPrivateChatActivity();

      // Hook 通用消息创建方法
      // AlipayHooker.this.hookCommonMessageCreation();

      // Hook 聊天回调类，此处包含主动收款
      // AlipayHooker.this.hookReceiveChatMessage();

      // 红包详情
      // AlipayHooker.this.hookRedpacketInfo();

      // Accessibility
      // AlipayHooker.this.hookSnsCouponDetailActivity();

      // Hook 聊天对象类(针对红包的)
          /*
          Class ChatMsgObj = classLoader.loadClass("com.alipay.mobile.socialcommonsdk.bizdata.chat.model.ChatMsgObj");
          Class SyncChatMsgModel = classLoader.loadClass("com.alipay.mobile.socialcommonsdk.bizdata.chat.model.SyncChatMsgModel");
          XposedHelpers.findAndHookConstructor(ChatMsgObj, String.class, SyncChatMsgModel, new ChatMsgObjCon(classLoader, context));

          final Class<?> sendMsg = AlipayHooker.this.findClass("com.alipay.mobile.socialchatsdk.chat.sender.request.BaseChatRequest", classLoader);
          final Class<?> redEn = AlipayHooker.this.findClass("com.alipay.mobile.redenvelope.proguard.n.a", classLoader);
          final Class<?> chatB = AlipayHooker.this.findClass("com.alipay.mobile.chatapp.ui.ChatMsgBaseActivity", classLoader);
          final Class<?> A = AlipayHooker.this.findClass("com.alipay.android.phone.discovery.envelope.ui.util.a", classLoader);
          */

      // 注册好友删除处理器
      // registerDeleteContactHandler();

      XposedBridge.log(TAG + ": 支付宝Hook成功。");
    } catch (Throwable e) {
      XposedBridge.log(TAG + ": 支付宝Hook失败。" + e.getMessage());
    }
  }

  /**
   * Hook 支付宝应用。
   */
  public void hookApplication() {
    XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
      @Override
      protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);

        AlipayHooker.mAppContext = (Context) param.args[0];
        ClassLoader appClassLoader = AlipayHooker.mAppContext.getClassLoader();
        // AlipayHooker.mClassLoader = appContext.getClassLoader();

        // Hook前安全检查，反侦查
        securityCheckHook(appClassLoader);

        String versionName = AlipayHooker.this.getAppVersion(AlipayHooker.mAppContext);
        XposedBridge.log(TAG + ": 支付宝版本: " + versionName);
      }
    });
  }

  /**
   * Hook 启动页面。在 onCreate 中注册广播接收器，在 onDestroy 中销毁广播接收器
   */
  public void hookLauncherActivity(XC_LoadPackage.LoadPackageParam lpparam) {
    try {
      XposedBridge.log(TAG + ": Hook 支付宝启动页开始...");

      XposedHelpers.findAndHookMethod("com.alipay.mobile.quinox.LauncherActivity", mClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
          mAlipayLauncherActivity = (Activity) param.thisObject;
          mAlipayBroadcastReceiver = new AlipayBroadcastReceiver();
          IntentFilter intentFilter = new IntentFilter();
          intentFilter.addAction(AlipayIntentActions.LaunchCollectUp);
          intentFilter.addAction(AlipayIntentActions.SetData);
          intentFilter.addAction(AlipayIntentActions.GetTradeInfo);
          mAlipayLauncherActivity.registerReceiver(mAlipayBroadcastReceiver, intentFilter);

          AlipayHooker.this.getUserLoginInfo();
          AlipayHooker.this.getUserCookie();
        }
      });

      XposedHelpers.findAndHookMethod("com.alipay.mobile.quinox.LauncherActivity", lpparam.classLoader, "onDestroy", new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
          XposedBridge.log(TAG + "支付宝退出，LauncherActivity 正在销毁...");

          if (mAlipayBroadcastReceiver != null) {
            ((Activity) param.thisObject).unregisterReceiver(mAlipayBroadcastReceiver);
          }

          mAlipayLauncherActivity = null;
        }
      });

      XposedBridge.log(TAG + ": Hook 支付宝启动页完成。");
    } catch (Exception e) {
      XposedBridge.log(TAG + ": Hook 支付宝启动页失败。" + e.getMessage());
    }
  }

  /**
   * Hook 消息插入操作。
   */
  public void hookMessageBoxServiceDaoInsertMessageInfo() {
    try {
      XposedBridge.log(TAG + ": Hook 支付宝 MessageBoxServiceDao.insertMessageInfo 开始...");

      String ServiceInfoClassName = "com.alipay.android.phone.messageboxstatic.biz.db.ServiceInfo";
      Class<?> ServiceInfo = XposedHelpers.findClass(ServiceInfoClassName, AlipayHooker.mClassLoader);

      String ServiceDaoClassName = "com.alipay.android.phone.messageboxstatic.biz.dao.ServiceDao";
      XposedHelpers.findAndHookMethod(ServiceDaoClassName, AlipayHooker.mClassLoader, "insertMessageInfo", ServiceInfo, String.class, new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
          try {
            String message = (String) XposedHelpers.callMethod(param.args[0], "toString", new Object[0]);
            XposedBridge.log(TAG + ": 支付宝收到支付订单消息: " + message);

            String extraInfo = StringUtils.getTextCenter(message, "extraInfo=\'", "\'").replace("\\", "");
            XposedBridge.log(TAG + ": 订单消息 extraInfo: " + extraInfo);

            /*String channelName = Channel.ALIPAY_QRCODE_FIX.getCode();
            String timeStamp = String.valueOf(System.currentTimeMillis());
            extraInfo = extraInfo.replace("\"bizMonitor\":\"", "\"bizMonitor\":").replace("}\",\"cardLink\"", "},\"cardLink\"");
            if((extraInfo.contains("你的银行卡")) || (extraInfo.contains("通过支付宝"))) {
              String v0_1 = PayUtils.getMidText(extraInfo, "转账", "元已到账");
              extraInfo = PayUtils.getMidText(extraInfo, "{\"assistMsg1\":\"", "通过支付宝");
              Log.d("arik", "支付宝收到支付订单======" + v0_1 + " mark==" + extraInfo);
              channelName = Channel.ALIPAY_NEW_ZK.getCode();
              extraInfo = v0_1;
            } else if(extraInfo.contains("店员通")) {
              channelName = Channel.ALIPAY_DY.getCode();
              timeStamp = StringUtils.getTextCenter(extraInfo, "mainAmount\":\"", "\",\"mainTitle");
              SimpleDateFormat v3 = new SimpleDateFormat("yyyyMMdd");
              extraInfo = JSON.parseObject(extraInfo).getString("assistMsg1");
              extraInfo = extraInfo.substring(0, extraInfo.indexOf("收款") + 1);
              StringBuilder v0 = new StringBuilder();
              v0.append(v3.format(new Date()));
              v0.append(extraInfo);
              v0.append(timeStamp);
              String v5 = timeStamp;
              timeStamp = v0.toString();
              extraInfo = v5;
            } else {
              JSONObject v7_2 = JSON.parseObject(extraInfo);
              JSON.parseObject(v7_2.getString("bizMonitor"));
              extraInfo = v7_2.getString("content");
              if(!extraInfo.contains("收款金额￥")) {
                Log.d("arik", "统计的支付消息，忽略..");
                return;
              } else {
                extraInfo = extraInfo.replace("收款金额￥", "");
              }
            }

            Intent v0_2 = new Intent();
            v0_2.putExtra("bill_no", timeStamp);
            v0_2.putExtra("bill_money", extraInfo);
            v0_2.putExtra("bill_mark", "");
            v0_2.putExtra("bill_time", System.currentTimeMillis());
            v0_2.putExtra("bill_type", channelName);
            v0_2.setAction(SealAppContext.BILLRECEIVED_ACTION);
            this.val$context.sendBroadcast(v0_2);*/
          } catch (Exception e) {
            e.printStackTrace();

            XposedBridge.log(TAG + ": 处理支付宝收到支付订单消息出错。" + e.getMessage());
          }
        }
      });

      XposedBridge.log(TAG + ": Hook 支付宝 MessageBoxServiceDao.insertMessageInfo 完成。");
    } catch (Exception e) {
      XposedBridge.log(TAG + ": Hook支付宝 MessageBoxServiceDao.insertMessageInfo 失败。" + e);
    }
  }

  /**
   * Hook 交易记录数据库的插入方法(即到账事件) TradeDao.insertMessageInfo。
   *
   * 当支付宝保存订单到本地数据库的时候，此方法会先执行，再将订单消息回调给服务端。
   */
  public void hookTradeDaoInsertMessageInfo() {
    try {
      XposedBridge.log(TAG + ": Hook支付宝交易记录插入方法 TradeDao.insertMessageInfo 开始...");

      Class<?> tradeDaoCls = AlipayHooker.this.findClass("com.alipay.android.phone.messageboxstatic.biz.dao.TradeDao");
      XposedBridge.hookAllMethods(tradeDaoCls, "insertMessageInfo", new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
          try {
            // 获取全部字段
            Object firstParam = param.args[0];
            String data = JSON.toJSONString(firstParam);
            XposedBridge.log(TAG + ": TradeDao.insertMessageInfo 第一个参数为 " + data);

            if (data.contains("收款到账")) {
              // 主动收款模式
              JSONObject paramJson = new JSONObject(data);
              JSONObject contentJson = new JSONObject(paramJson.getString("content"));
              String fromUserId = paramJson.optString("userId");
              String remark = contentJson.getString("assistMsg2");
              String receiveAmount = contentJson.getString("content").replace("￥", "");
              String alipayMsgId = paramJson.optString("msgId");

              XposedBridge.log(TAG + ": TradeDao.insertMessageInfo 订单ID: " + remark + "，付款方userId: " + fromUserId);

              // 回调，以广播的形式发送出去
              Map<String, Object> orderInfo = new HashMap<>();
              orderInfo.put("remark", remark);
              orderInfo.put("amount", receiveAmount);
              orderInfo.put("payerUserId", fromUserId);
              orderInfo.put("alipayMsgId", alipayMsgId);

              Intent intent = new Intent();
              intent.setAction(PluginIntentActions.AlipayCollectUp);
              intent.putExtra("data", JSON.toJSONString(orderInfo));

              XposedBridge.log(TAG + ": 收款到账广播开始...");
              mAlipayLauncherActivity.sendBroadcast(intent);
              XposedBridge.log(TAG + ": 收款到账广播成功.");
            } else if (data.contains("二维码收款") || data.contains("收到一笔转账") || data.contains("成功收款")){
              // 转账模式
            }

            XposedBridge.log(TAG + ": TradeDao.insertMessageInfo 结束");
          } catch (Exception e) {
            XposedBridge.log(e.getMessage());
          }

          super.beforeHookedMethod(param);
        }
      });

      XposedBridge.log(TAG + ": Hook支付宝交易记录插入方法 TradeDao.insertMessageInfo 完成。");
    } catch (Exception e) {
      XposedBridge.log(TAG + ": Hook支付宝交易记录插入方法 TradeDao.insertMessageInfo 失败。" + e);
    }
  }

  /**
   * Hook 私聊Activity
   */
  public void hookPrivateChatActivity() {
    try {
      XposedBridge.log(TAG + ": Hook 私聊 Activity 开始...");

      Class<?> PersonalChatMsgActivity_ = AlipayHooker.this.findClass("com.alipay.mobile.chatapp.ui.PersonalChatMsgActivity_");
      XposedHelpers.findAndHookMethod(PersonalChatMsgActivity_, "onCreate", Bundle.class, new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
          super.afterHookedMethod(param);

          Intent intent = ((Activity) param.thisObject).getIntent();
          Bundle bundle = intent.getExtras();
          Set<String> set = bundle.keySet();
          for (String string : set) {
            XposedBridge.log("key=" + string + "--value=" + bundle.get(string));
          }
        }
      });

      XposedBridge.log(TAG + ": Hook 私聊 Activity 完成。");
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取支付宝联系人账号失败。" + e);
    }
  }

  /**
   * Hook 通用消息创建方法。
   */
  public void hookCommonMessageCreation() {
    try {
      XposedBridge.log(TAG + ": Hook MessageFactory.createCommonMessage 开始...");

      final Class<?> msgFac = AlipayHooker.this.findClass("com.alipay.mobile.socialchatsdk.chat.sender.MessageFactory");
      XposedBridge.hookAllMethods(msgFac, "createCommonMessage", new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
          XposedBridge.log(TAG + ": createCommonMessage " + Arrays.toString(param.args));
          if (param.args.length >= 4) {
            Object o = param.args[3];
            if (o.getClass().toString().contains("CommonMediaInfo")) {
              XposedBridge.log("CommonMediaInfo " + JSON.toJSONString(o));
            }
          }
        }
      });

      XposedBridge.log(TAG + ": Hook MessageFactory.createCommonMessage 完成。");
    } catch (Exception e) {
      XposedBridge.log(TAG + ": Hook MessageFactory.createCommonMessage 失败。" + e);
    }
  }

  /**
   * Hook 聊天信息接收方法。
   */
  public void hookReceiveChatMessage() {
    try {
      XposedBridge.log(TAG + ": Hook ChatDataSyncCallback.onReceiveMessage 开始...");

      final Class<?> wire = AlipayHooker.this.findClass("com.squareup.wire.Wire");
      final Class<?> SyncMessage = AlipayHooker.this.findClass("com.alipay.mobile.rome.longlinkservice.syncmodel.SyncMessage");
      final Class<?> msgPModel = AlipayHooker.this.findClass("com.alipay.mobilechat.core.model.message.MessagePayloadModel");
      Class<?> chatCallback = AlipayHooker.this.findClass("com.alipay.mobile.socialchatsdk.chat.data.ChatDataSyncCallback");
      XposedHelpers.findAndHookMethod(chatCallback, "onReceiveMessage", SyncMessage,
          new XC_MethodHook() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
              super.afterHookedMethod(param);

              Object object = param.args[0];
              JSONArray msgDataArray = new JSONArray(SyncMessage.getField("msgData").get(object).toString());
              XposedBridge.log("[支付宝Hook]收到聊天消息: " + msgDataArray);

              String msgPayload = msgDataArray.getJSONObject(0).getString("pl");

              // 将内容使用Base64解码后，再转换为JSON对象
              Object wireIns = XposedHelpers.newInstance(wire, new ArrayList<Class>());
              Object decodedMsgPayload = XposedHelpers.callMethod(wireIns, "parseFrom", Base64.decode(msgPayload, 0), msgPModel);
              String decodeMsgStr = JSON.toJSONString(decodedMsgPayload);

              XposedBridge.log("[支付宝Hook]聊天消息解码: " + decodeMsgStr);

              // 提取需要的数据内容
              com.alibaba.fastjson.JSONObject decodedMsg = JSON.parseObject(decodeMsgStr);
              String alipayBizType = decodedMsg.getString("biz_type");
              String content = decodedMsg.getJSONObject("template_data").getString("m");
              final String fromUserId = decodedMsg.getString("from_u_id");
              String link = decodedMsg.getString("link") + "#";
              String mUserId = decodedMsg.getString("to_u_id");

              boolean universalDetail = true;
              String socialCardCMsgId = decodedMsg.getString("client_msg_id");
              String target = "groupPre";
              String schemeMode = "portalInside";
              String prevBiz = "chat";
              String bizType = "CROWD_COMMON_CASH";
              String sign = StringUtils.getTextCenter(link, "sign=", "#");
              String appId = "88886666";
              boolean REALLY_START_APP = true;
              String chatUserType = "1";
              String clientVersion = "10.0.0-5";
              boolean startFromExternal = false;
              String crowdId = StringUtils.getTextCenter(link, "crowdNo=", "&");
              String socialCardToUserId = decodedMsg.getString("to_u_id");
              boolean appClearTop = false;
              boolean REALLY_DO_START_APP = true;
              String ap_framework_sceneId = "20000167";

              XposedBridge.log("[支付宝Hook]聊天消息类型: biz_type=" + alipayBizType + ", content=" + content);

              if ("COLLET".equals(alipayBizType) && content != null && content.contains("向你支付")) {
                // 主动收款请求, 对方支付成功
                XposedBridge.log("[支付宝Hook]收款成功，即将删除好友: " + fromUserId);

                Message message = new Message();
                message.what = 1;
                message.obj = fromUserId;
                deleteContactHandler.sendMessageDelayed(message, 10000);
              } else if (alipayBizType.equals("CHAT")) {
                // 普通聊天信息，分解出金额(money)与备注(订单ID)，根据此信息向用户发送收款请求
                if (content.contains("~") && content.length() >= 15) {
                  try {
                    // 金额使用Base64编码
                    byte[] buffer = java.util.Base64.getDecoder().decode(content.split("~")[0]);
                    if (buffer != null) {
                      String money = new String(buffer, "utf-8");
                      String remark = content.split("~")[1];

                      AlipayHooker.this.sendCollectUpMessageToPayee(fromUserId, money, remark);
                    }
                  } catch (UnsupportedEncodingException e) {
                    XposedBridge.log("[支付宝Hook]订单金额Base64解码失败: " + content);
                  }
                }
              } else if (alipayBizType.equals("GIFTSHARE")) {
                // 礼物？
                Class<?> snsCou = AlipayHooker.this.findClass("com.alipay.android.phone.discovery.envelope.get.SnsCouponDetailActivity");
                Intent envIntent = new Intent(mAppContext, snsCou);

                Bundle bundle = new Bundle();
                bundle.putString("chatUserId", fromUserId);
                bundle.putString("socialCardCMsgId", socialCardCMsgId);
                bundle.putBoolean("universalDetail", universalDetail);
                bundle.putString("target", target);
                bundle.putString("schemeMode", schemeMode);
                bundle.putString("prevBiz", prevBiz);
                bundle.putString("bizType", bizType);
                bundle.putString("sign", sign);
                bundle.putString("appId", appId);
                bundle.putString("chatUserType", chatUserType);
                bundle.putString("clientVersion", clientVersion);
                bundle.putBoolean("startFromExternal", startFromExternal);
                bundle.putString("crowdNo", crowdId);
                bundle.putString("socialCardToUserId", socialCardToUserId);
                bundle.putBoolean("appClearTop", appClearTop);
                bundle.putBoolean("REALLY_STARTAPP", REALLY_START_APP);
                bundle.putBoolean("REALLY_DOSTARTAPP", REALLY_DO_START_APP);
                bundle.putString("ap_framework_sceneId", ap_framework_sceneId);

                bundleList.add(bundle);
              }
            }
          });

      XposedBridge.log(TAG + ": Hook ChatDataSyncCallback.onReceiveMessage 完成。");
    } catch (Exception e) {
      XposedBridge.log(TAG + ": Hook ChatDataSyncCallback.onReceiveMessage 失败。" + e);
    }
  }

  /**
   * Hook 红包详情。
   */
  public void hookRedpacketInfo() {
    try {
      XposedBridge.log(TAG + ": Hook 红包详情开始...");

      final Class<?> giftCrow = AlipayHooker.this.findClass("com.alipay.giftprod.biz.crowd.gw.result.GiftCrowdDetailResult");
      final Class<?> snsCou = AlipayHooker.this.findClass("com.alipay.android.phone.discovery.envelope.get.SnsCouponDetailActivity");

      XposedHelpers.findAndHookMethod(snsCou, "a", giftCrow, boolean.class, new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
          super.afterHookedMethod(param);
          String s = JSON.toJSONString(param.args[0]);
          com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(s);

          XposedBridge.log(">>>>>:" + jsonObject.getJSONObject("giftCrowdInfo").toString());

          String remark = jsonObject.getJSONObject("giftCrowdInfo").getString("remark");
          String amount = jsonObject.getJSONObject("giftCrowdInfo").getString("amount");
          String mNo = jsonObject.getJSONObject("giftCrowdInfo").getString("crowdNo");
          XposedBridge.log("mMark>>>>>:" + remark);
        }
      });

      XposedBridge.log(TAG + ": Hook 红包详情完成。");
    } catch (Exception e) {
      XposedBridge.log(TAG + ": Hook 红包详情失败。" + e);
    }
  }

  /**
   * Hook 红包详情。
   */
  public void hookSnsCouponDetailActivity() {
    try {
      XposedBridge.log(TAG + ": Hook SnsCouponDetailActivity 开始...");

      final Class<?> snsCou = AlipayHooker.this.findClass("com.alipay.android.phone.discovery.envelope.get.SnsCouponDetailActivity");
      XposedHelpers.findAndHookMethod(snsCou, "a", Context.class, new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
          return false;
        }
      });

      XposedBridge.log(TAG + ": Hook SnsCouponDetailActivity 完成。");
    } catch (Exception e) {
      XposedBridge.log(TAG + ": Hook SnsCouponDetailActivity 失败。" + e);
    }
  }

  /**
   * 向付款方发送主动收款消息。顺便把交易ID也传给服务端，以便能够完成自动拉起。
   */
  public String sendCollectUpMessageToPayee(String targetUserId, String money, String remark) {
    Object alipayApp = this.getAlipayApp();
    Object service = this.getAlipayRpcService();

    // XposedBridge.log("[支付宝Hook]service: " + service);

    Object bundleContext = XposedHelpers.callMethod(alipayApp, "getBundleContext");
    ClassLoader classLoader = (ClassLoader) XposedHelpers.callMethod(bundleContext, "findClassLoaderByBundleName", "android-phone-wallet-socialpayee");
    Object SingleCollectRpc = XposedHelpers.callMethod(service, "getRpcProxy", AlipayHooker.this.findClass("com.alipay.android.phone.personalapp.socialpayee.rpc.SingleCollectRpc"));

    // XposedBridge.log("[支付宝Hook]SingleCollectRpc: " + SingleCollectRpc);

    Object contactAccount = this.getContactAccount(targetUserId);
    String name = XposedHelpers.getObjectField(contactAccount, "name") + "";
    String userId = XposedHelpers.getObjectField(contactAccount, "userId") + "";
    String logonId = XposedHelpers.getObjectField(contactAccount, "account") + "";
    Object singleCreateReq = XposedHelpers.newInstance(AlipayHooker.this.findClass("com.alipay.android.phone.personalapp.socialpayee.rpc.req.SingleCreateReq"));

    // XposedBridge.log("[支付宝Hook]主动收款请求对象: " + JSON.toJSONString(singleCreateReq));

    /*
    Field[] fields = singleCreateReq.getClass().getDeclaredFields();
    for (Field field : fields) {
      XposedBridge.log("[支付宝Hook]主动收款请求字段：" + field.getName() + ", 类型：" + field.getType());
    }
    */

    XposedHelpers.setObjectField(singleCreateReq, "userName", name);
    XposedHelpers.setObjectField(singleCreateReq, "userId", userId);
    XposedHelpers.setObjectField(singleCreateReq, "logonId", logonId);
    XposedHelpers.setObjectField(singleCreateReq, "desc", remark);
    XposedHelpers.setObjectField(singleCreateReq, "payAmount", money);
    XposedHelpers.setObjectField(singleCreateReq, "billName", "个人收款");
    XposedHelpers.setObjectField(singleCreateReq, "source", "chat");
    Object bill = XposedHelpers.callMethod(SingleCollectRpc, "createBill", singleCreateReq);

    XposedBridge.log("[支付宝Hook]主动收款发送结果: " + JSON.toJSONString(bill));

    return JSON.toJSONString(bill);
  }

  /**
   * Hook 前检查。解决支付宝的反hook
   */
  protected void securityCheckHook(ClassLoader appClassLoader) {
    try {
      Class<?> securityCheckClazz = XposedHelpers.findClass("com.alipay.mobile.base.security.CI", appClassLoader);
      XposedHelpers.findAndHookMethod(securityCheckClazz, "a", String.class, String.class, String.class, new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
          Object object = param.getResult();
          XposedHelpers.setBooleanField(object, "a", false);
          param.setResult(object);

          super.afterHookedMethod(param);
        }
      });

      XposedHelpers.findAndHookMethod(securityCheckClazz, "a", Class.class, String.class, String.class, new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
          return (byte) 1;
        }
      });

      XposedHelpers.findAndHookMethod(securityCheckClazz, "a", ClassLoader.class, String.class, new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
          return (byte) 1;
        }
      });

      XposedHelpers.findAndHookMethod(securityCheckClazz, "a", new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
          return false;
        }
      });
    } catch (Error | Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 获取联系人信息。
   */
  public Object getContactAccount(String userId) {
    try {
      Object ApplicationContext = this.getAlipayAppContext();

      Object socialSdkContactService = XposedHelpers.callMethod(ApplicationContext, "getExtServiceByInterface", "com.alipay.mobile.personalbase.service.SocialSdkContactService");
      Object ContactAccount = XposedHelpers.callMethod(socialSdkContactService, "queryAccountById", userId);

      XposedBridge.log(TAG + ": 获取联系人账号信息 " + JSON.toJSONString(ContactAccount));

      return ContactAccount;
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取支付宝联系人账号失败。" + e);
    }

    return null;
  }

  /**
   * 删除联系人。
   */
  private void deleteContact(String toDeleteUserId) {
    Object targetAccount = this.getContactAccount(toDeleteUserId);

    Object handleRelationReq = XposedHelpers.newInstance(AlipayHooker.this.findClass("com.alipay.mobilerelation.biz.shared.req.HandleRelationReq"));
    String userId = XposedHelpers.getObjectField(targetAccount, "userId") + "";
    String account = XposedHelpers.getObjectField(targetAccount, "account") + "";

    XposedHelpers.setObjectField(handleRelationReq, "targetUserId", userId);
    XposedHelpers.setObjectField(handleRelationReq, "alipayAccount", account);
    XposedHelpers.setObjectField(handleRelationReq, "bizType", "2");

    Object rpcService = this.getAlipayRpcService();
    Class<?> AlipayRelationManageService = AlipayHooker.this.findClass("com.alipay.mobilerelation.biz.shared.rpc.AlipayRelationManageService");
    Object alipayRelationManageService = XposedHelpers.callMethod(rpcService, "getRpcProxy", AlipayRelationManageService);
    Object res = XposedHelpers.callMethod(alipayRelationManageService, "handleRelation", handleRelationReq);

    XposedBridge.log(TAG + ": 好友删除完成。" + JSON.toJSONString(res));
  }

  /**
   * 获取交易记录Dao。
   */
  public Object getTradeDao() {
    try {
      Class<?> TradeDao = this.findClass("com.alipay.android.phone.messageboxstatic.biz.dao.TradeDao");
      Object tradeDao = XposedHelpers.callStaticMethod(TradeDao, "getInstance");

      XposedBridge.log(TAG + ": 获取 TradeDao 成功。实现类：" + tradeDao.getClass().getName());

      return tradeDao;
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取 TradeDao 失败。" + e);
    }

    return null;
  }

  /**
   * 获取聊天信息Dao。
   */
  public Object getChatDao() {
    try {
      Class<?> ChatMsgDao = this.findClass("com.alipay.mobile.socialcommonsdk.bizdata.chat.data.ChatMsgDaoOp");
      Object chatMsgDao = XposedHelpers.callStaticMethod(ChatMsgDao, "getInstance", new Object[0]);

      XposedBridge.log(TAG + ": 获取 ChatMsgDao 成功。实现类：" + chatMsgDao.getClass().getName());

      return chatMsgDao;
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取 ChatMsgDao 失败。" + e);
    }

    return null;
  }

  /**
   * 注册一个好友删除处理器。
   */
  @SuppressLint("HandlerLeak")
  private void registerDeleteContactHandler() {
    deleteContactHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        final String userId = (String) msg.obj;
        new Thread(new Runnable() {
          @Override
          public void run() {
            deleteContact(userId);
          }
        }).start();
      }
    };
  }

  /**
   * 获取支付宝应用程序对象。全局唯一实例。
   */
  private Object getAlipayApp() {
    try {
      Class AlipayApplication = this.findClass("com.alipay.mobile.framework.AlipayApplication");
      Object alipayApp = XposedHelpers.callStaticMethod(AlipayApplication, "getInstance");

      // XposedBridge.log(TAG + ": 获取支付宝应用文对象成功，实现类名称为 " + alipayApp.getClass().getName());

      return alipayApp;
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取支付宝应用对象失败。" + e.getMessage());
    }

    return null;
  }

  /**
   * 获取支付宝应用程序对象上下文。全局唯一实例。
   */
  private Object getAlipayAppContext() {
    try {
      Object alipayApp = this.getAlipayApp();
      Object alipayAppContext = XposedHelpers.callMethod(alipayApp, "getMicroApplicationContext");

      // XposedBridge.log(TAG + ": 获取支付宝应用上下文对象成功，实现类名称为 " + alipayAppContext.getClass().getName());

      return alipayAppContext;
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取支付宝宝应用上下文对象失败。" + e.getMessage());
    }

    return null;
  }

  /**
   * 获取支付宝RPC服务对象。
   */
  public Object getAlipayRpcService() {
    try {
      Class RpcService = this.findClass("com.alipay.mobile.framework.service.common.RpcService");

      XposedBridge.log(TAG + ": 获取支付宝RPC服务对象成功，实现类名称为 " + RpcService.getClass().getName());

      return XposedHelpers.callMethod(this.getAlipayAppContext(), "findServiceByInterface", RpcService.getName());
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取支付宝RPC服务对象失败。" + e);
    }

    return null;
  }

  /**
   * 获取当前登录用户社交信息。
   */
  public Object getUserSocialInfo() {
    try {
      XposedBridge.log(TAG + ": 开始获取用户社交信息。");

      Object appContext = this.getAlipayAppContext();
      Class<?> SocialSdkContactService = this.findClass("com.alipay.mobile.personalbase.service.SocialSdkContactService");
      Object service = XposedHelpers.callMethod(appContext, "findServiceByInterface", SocialSdkContactService.getName());
      Object socialAccountInfo = XposedHelpers.callMethod(service, "getMyAccountInfoModelByLocal");
      XposedBridge.log(TAG + ": SocialSdkContactService 取得的用户社交信息: " + JSON.toJSONString(socialAccountInfo));

      // 以广播的形式发送出去
      Intent intent = new Intent();
      intent.setAction(PluginIntentActions.AlipaySocialInfoFetched);
      intent.putExtra("data", JSON.toJSONString(socialAccountInfo));
      mAppContext.sendBroadcast(intent);

      return socialAccountInfo;
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取用户社交信息失败。" + e);
    }

    return null;
  }

  /**
   * 获取当前登录用户信息。
   */
  public Map<String, Object> getUserLoginInfo() {
    try {
      XposedBridge.log(TAG + ": 开始获取用户登录信息。");

      Class<?> UserInfoHelper = this.findClass("com.alipay.mobile.common.helper.UserInfoHelper");
      Object userInfoHelperInst = XposedHelpers.callStaticMethod(UserInfoHelper, "getInstance");
      Object userInfo = XposedHelpers.callMethod(userInfoHelperInst, "getUserInfo", this.getAlipayAppContext());
      if (userInfo == null) {
        XposedBridge.log(TAG + ": 获取用户登录信息失败。");
        return null;
      }

      // XposedBridge.log(TAG + ": UserInfoHelper 取得的用户信息: " + JSON.toJSONString(userInfo));

      com.alibaba.fastjson.JSONObject userInfoJson = JSON.parseObject(JSON.toJSONString(userInfo));
      Object userId = userInfoJson.get("userId");
      Object logonId = userInfoJson.get("logonId");
      Object realName = userInfoJson.get("realName");
      Object nickname = userInfoJson.get("showName");
      Object userAvatar = userInfoJson.get("userAvatar");

      Map<String, Object> loginUserInfo = new HashMap<>();
      loginUserInfo.put("userId", userId);
      loginUserInfo.put("logonId", logonId);
      loginUserInfo.put("nickname", nickname);
      loginUserInfo.put("realName", realName);
      loginUserInfo.put("headimgUrl", userAvatar);

      XposedBridge.log(TAG + ": 获取用户登录信息成功。" + JSON.toJSONString(loginUserInfo));

      // 以广播的形式发送出去
      Intent intent = new Intent();
      intent.setAction(PluginIntentActions.AlipayUserInfoFetched);
      intent.putExtra("data", JSON.toJSONString(loginUserInfo));

      XposedBridge.log(TAG + ": 用户信息广播开始...");
      mAlipayLauncherActivity.sendBroadcast(intent);
      XposedBridge.log(TAG + ": 用户信息广播成功.");

      return loginUserInfo;
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取用户登录信息失败。" + e);
    }

    return null;
  }

  /**
   * 获取登录的支付宝Cookie。
   */
  public String getUserCookie() {
    String alipayCookie = "";

    try {
      Class<?> AmnetUserInfo = this.findClass("com.alipay.mobile.common.transportext.biz.appevent.AmnetUserInfo");
      XposedHelpers.callStaticMethod(AmnetUserInfo, "getSessionid");

      Class<?> ExtTransportEnv = this.findClass("com.alipay.mobile.common.transportext.biz.shared.ExtTransportEnv");
      Context context = (Context) XposedHelpers.callStaticMethod(ExtTransportEnv, "getAppContext");

      if (context != null) {
        Class<?> ReadSettingServerUrl = this.findClass("com.alipay.mobile.common.helper.ReadSettingServerUrl");
        Object readSettingServerUrl = XposedHelpers.callStaticMethod(ReadSettingServerUrl, "getInstance");
        if (readSettingServerUrl != null) {
          String gWFURL = ".alipay.com";

          Class<?> GwCookieCacheHelper = this.findClass("com.alipay.mobile.common.transport.http.GwCookieCacheHelper");
          alipayCookie = (String) XposedHelpers.callStaticMethod(GwCookieCacheHelper, "getCookie", gWFURL);
        }
      }

      if (alipayCookie.equals("")) {
        XposedBridge.log(TAG + ": 获取用户Cookie失败。");
      } else {
        XposedBridge.log(TAG + ": 获取用户Cookie成功。" + alipayCookie);

        // 以广播的形式发送出去
        Intent intent = new Intent();
        intent.setAction(PluginIntentActions.AlipayCookieFetched);
        intent.putExtra("data", alipayCookie);

        XposedBridge.log(TAG + ": 用户Cookie广播开始...");
        mAlipayLauncherActivity.sendBroadcast(intent);
        XposedBridge.log(TAG + ": 用户Cookie广播成功.");
      }
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取用户Cookie失败。" + e.getMessage());
    }

    return alipayCookie;
  }
}
