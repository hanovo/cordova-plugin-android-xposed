package com.skynet.xposed.hookers.alipay;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import com.skynet.xposed.hookers.BaseAppHooker;
import com.skynet.xposed.hookers.HookableApps;
import com.skynet.xposed.utils.IntentActions;
import com.skynet.xposed.utils.PayHelperUtils;
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

/**
 * 支付宝钩子。
 */
public class AlipayHooker extends BaseAppHooker {
  private static String TAG = AlipayHooker.class.getSimpleName();

  private static AlipayHooker inst = null;
  private static List<Bundle> bundleList = new ArrayList<>();

  private Activity mAlipayActivity = null;
  private Handler deleteContactHandler = null;  // 好删除消息处理器

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
      if (com.skynet.xposed.hookers.HookableApps.inst().isHooked(HookableApps.PackageAlipay)) return;

      XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
          super.afterHookedMethod(param);

          AlipayHooker.this.mAlipayActivity = (Activity) param.thisObject;
          XposedBridge.log(TAG + ": 当前 Activity 为 " + mAlipayActivity.getClass().toString());

          // 若非支付宝未登录页面，直接返回
          String cm = mAlipayActivity.getClass().getSimpleName();
          if (!cm.equals("AlipayLogin")) return;

          AlipayHooker.this.mClassLoader = mAlipayActivity.getClassLoader();

          // 在支付宝首页，注册广播事件接收器
          IntentFilter intentFilter = new IntentFilter();
          intentFilter.addAction(IntentActions.AlipayLaunchCollectUp);
          intentFilter.addAction(IntentActions.AlipaySetData);

          AlipayReceiver alipayReceiver = new AlipayReceiver();
          mAlipayActivity.registerReceiver(alipayReceiver, intentFilter);

          // Hook前安全检查
          securityCheckHook();

          // timer.schedule(task, 0, 1000);

          // 注册一个处理器，用于删除好友
          registerDeleteContactHandler();

          XposedBridge.log(TAG + ": 开始支付宝Hook...");

          // Hook TradeDao 插入方法(即到账的时候执行)
          AlipayHooker.this.hookTradeDaoInsertMessageInfo();

          // Hook 私聊 Activity
          AlipayHooker.this.hookPrivateChatActivity();

          // Hook 通用消息创建方法
          AlipayHooker.this.hookCommonMessageCreation();

          // Hook 聊天回调类，此处包含主动收款
          AlipayHooker.this.hookReceiveChatMessage();

          // 红包详情
          AlipayHooker.this.hookRedpacketInfo();

          // Accessibility
          AlipayHooker.this.hookSnsCouponDetailActivity();

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

          com.skynet.xposed.hookers.HookableApps.inst().addHookedApp(HookableApps.PackageAlipay);

          String versionName = AlipayHooker.this.getAppVersion(AlipayHooker.this.mAlipayActivity);
          XposedBridge.log(TAG + ": 支付宝Hook成功，版本: " + versionName);
          // PayHelperUtils.sendMsg(mAlipayActivity, "支付宝Hook成功，版本: " + versionName);
        }
      });
    } catch (Throwable e) {
      XposedBridge.log("[支付宝Hook]失败:(" + e.getMessage());
    }

    /* 另一个版本的Hook过程
    if (lpparam.appInfo != null && (lpparam.appInfo.flags & 129) == 0) {
      try {
        String processName = lpparam.processName;
        XposedHelpers.findAndHookMethod(Application.class, "attach", new Object[]{Context.class, new XC_MethodHook(processName) {
          protected void afterHookedMethod(MethodHookParam arg5) throws Throwable {
            super.afterHookedMethod(arg5);
            Object v5 = arg5.args[0];
            HookMain.ALIHOOK_CONTEXT = ((Context) v5);
            ClassLoader packageName = ((Context) v5).getClassLoader();
            if ((HookableApps.PackageAlipay.equals(this.val$processName)) && !HookMain.this.ALIPAY_PACKAGE_ISHOOK) {
              HookMain.this.ALIPAY_PACKAGE_ISHOOK = true;
              AlipayReceived v1 = new AlipayReceived(HookMain.this);
              IntentFilter v2 = new IntentFilter();
              v2.addAction("com.alipay.kouling");
              ((Context) v5).registerReceiver(((BroadcastReceiver) v1), v2);
              new AlipayHook().hook(packageName, ((Context) v5));
              PayHelperUtils.sendmsg(((Context) v5), "支付宝Hook成功，当前版本:" + PayHelperUtils.getVerName(((Context) v5)));
            }
          }
        }});
      } catch (Throwable v6_1) {
        Log.d(TAG, "[ERR]: Hook支付宝错误：" + v6_1.getMessage());
        Context v0_1 = HookMain.ALIHOOK_CONTEXT;
        PayHelperUtils.sendmsg(v0_1, "Hook支付宝错误：" + v6_1.getMessage());
      }
    }*/
  }

  /**
   * Hook 交易记录数据库的插入方法(即到账事件) TradeDao.insertMessageInfo。
   * <p>
   * 当支付宝保存订单到本地数据库的时候，此方法会先执行，再将订单消息回调给服务端。
   */
  public void hookTradeDaoInsertMessageInfo() {
    try {
      XposedBridge.log(TAG + ": 开始Hook支付宝交易记录插入方法 TradeDao.insertMessageInfo...");

      Class<?> tradeDaoCls = AlipayHooker.this.findClass("com.alipay.android.phone.messageboxstatic.biz.dao.TradeDao");
      XposedBridge.hookAllMethods(tradeDaoCls, "insertMessageInfo", new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
          try {
            // 获取全部字段
            Object object = param.args[0];
            XposedBridge.log(TAG + ": Hook支付宝交易记录插入方法 TradeDao.insertMessageInfo，参数为 " + JSON.toJSONString(object));

            // PayHelperUtils.sendMsg(context, (String) XposedHelpers.getObjectField(param.args[0], "content"));

            String content = (String) XposedHelpers.callMethod(object, "toString");
            if (content.contains("收款到账")) {
              JSONObject jsonObject = new JSONObject(JSON.toJSONString(object));
              JSONObject contentJson = new JSONObject(jsonObject.getString("content"));
              String fromUserId = jsonObject.optString("userId");
              String receiveAmount = contentJson.getString("content").replace("￥", "");
              String remark = contentJson.getString("assistMsg2");
              String alipayMsgId = jsonObject.optString("msgId");
              XposedBridge.log(TAG + ": TradeDao.insertMessageInfo 订单ID: " + remark + "，发送者userId: " + fromUserId);

              // todo: 回调
              /*
              final String mNotifyUrl = CommonUtil.readConfigFromFile("callbackUrl", "");
              String mSignToken = CommonUtil.readConfigFromFile("signToken", "");
              String dt = System.currentTimeMillis() + "";
              String sign = MD5.digest(dt + remark + receiveAmount + alipayMsgId + "alipay" + mSignToken + fromUserId);

              RequestParams params = new RequestParams();
              params.addBodyParameter("remark", remark);
              params.addBodyParameter("channel", "alipay-collect-up");
              params.addBodyParameter("receiveAmount", receiveAmount);
              params.addBodyParameter("fromUserId", fromUserId);
              params.addBodyParameter("alipayMsgId", alipayMsgId);
              params.addBodyParameter("signToken", mSignToken);
              params.addBodyParameter("dt", dt);
              params.addBodyParameter("sign", sign);

              HttpUtils httpUtils = new HttpUtils(30000);
              httpUtils.send(HttpRequest.HttpMethod.POST, mNotifyUrl, params, new RequestCallBack<String>() {
                @Override
                public void onFailure(HttpException arg0, String arg1) {
                  // PayHelperUtils.sendMsg(context, "[支付宝Hook]收款回调失败：" + arg1);
                  XposedBridge.log(TAG + ": 收款回调失败。" + arg1);
                }

                @Override
                public void onSuccess(ResponseInfo<String> res) {
                  String data = res.result;

                  // PayHelperUtils.sendMsg(context, "[支付宝Hook]收款回调成功：" + data);
                  XposedBridge.log(TAG + ": 收款回调成功。" + data);
                }
              });*/
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
      XposedBridge.log(TAG + ": Hook私聊 Activity 开始...");

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

      XposedBridge.log(TAG + ": Hook私聊 Activity 完成。");
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
                Intent envIntent = new Intent(mAlipayActivity, snsCou);

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
                PayHelperUtils.sendMsg(mAlipayActivity, bundleList.size() + "");
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
   * Hook 前检查。
   */
  protected void securityCheckHook() {
    try {
      Class<?> securityCheckClazz = this.findClass("com.alipay.mobile.base.security.CI");
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
   * 获取当前登录用户信息。
   */
  public Map<String, Object> getLoginUserInfo() {
    try {
      /* 另一个获取用户信息的方法。相比另一个，这个需要打开指定的页面才可以获取，比较麻烦。
      XposedHelpers.findAndHookMethod("com.alipay.mobile.quinox.LauncherActivity", mClassLoader, "onResume",
        new XC_MethodHook() {
          @Overridec
          protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            PayHelperUtils.isFirst = true;

            String loginId = PayHelperUtils.getAlipayLoginId(classLoader);
            PayHelperUtils.sendLoginId(context, "alipay", loginId);
          }
        });
      */

      /*
      String loginId = "";
      Class<?> AlipayApplication = XposedHelpers.findClass("com.alipay.mobile.framework.AlipayApplication", classLoader);
      Class<?> SocialSdkContactService = XposedHelpers.findClass("com.alipay.mobile.personalbase.service.SocialSdkContactService", classLoader);
      Object instace = XposedHelpers.callStaticMethod(AlipayApplication, "getInstance");
      Object MicroApplicationContext = XposedHelpers.callMethod(instace, "getMicroApplicationContext");
      Object service = XposedHelpers.callMethod(MicroApplicationContext, "findServiceByInterface", SocialSdkContactService.getName());
      Object MyAccountInfoModel = XposedHelpers.callMethod(service, "getMyAccountInfoModelByLocal");
      loginId = XposedHelpers.getObjectField(MyAccountInfoModel, "loginId").toString();
      */

      Class UserInfoHelper = this.findClass("com.alipay.mobile.common.helper.UserInfoHelper");
      Object userInfoHelperInst = XposedHelpers.callStaticMethod(UserInfoHelper, "getInstance", new Object[0]);
      Object userInfo = XposedHelpers.callMethod(userInfoHelperInst, "getUserInfo", this.getAlipayAppContext());
      if (userInfo == null) return null;

      Object logonId = XposedHelpers.callMethod(userInfo, "getLogonId", new Object[0]);
      Object userId = XposedHelpers.callMethod(userInfo, "getUserId", new Object[0]);
      Object nickname = XposedHelpers.callMethod(userInfo, "getNick", new Object[0]);
      Object realName = XposedHelpers.callMethod(userInfo, "getRealName", new Object[0]);
      Object userAvatar = XposedHelpers.callMethod(userInfo, "getUserAvatar", new Object[0]);

      Map<String, Object> ret = new HashMap<>();
      ret.put("logonId", logonId);
      ret.put("userId", userId);
      ret.put("nickname", nickname);
      ret.put("realName", realName);
      ret.put("userAvatar", userAvatar);

      //PayHelperUtils.sendLoginId(((Map) v2), ((String) v0), ((String) v1), "alipay", arg8);
      XposedBridge.log(TAG + ": 获取用户登录信息成功。" + JSON.toJSONString(ret));

      return ret;
    } catch (Exception e) {
      XposedBridge.log(TAG + ": 获取用户登录信息失败。" + e);
    }

    return null;
  }

  /**
   * 获取交易记录Dao。
   */
  public Object getTradeDao() {
    try {
      Class<?> TradeDao = this.findClass("com.alipay.android.phone.messageboxstatic.biz.dao.TradeDao");
      Object tradeDao = XposedHelpers.callStaticMethod(TradeDao, "getInstance", new Object[0]);

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

      XposedBridge.log(TAG + ": 获取支付宝应用文对象成功，实现类名称为 " + alipayApp.getClass().getName());

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

      XposedBridge.log(TAG + ": 获取支付宝应用上下文对象成功，实现类名称为 " + alipayAppContext.getClass().getName());

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
   * 获取登录的支付宝Cookie。
   */
  public String getAlipayCookie() {
    String alipayCookie = "";

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

    return alipayCookie;
  }
}