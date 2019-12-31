package com.skynet.xposed.cordova.plugin;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.lidroid.xutils.http.RequestParams;
import com.skynet.xposed.utils.AbSharedUtil;
import com.skynet.xposed.utils.MD5;
import com.skynet.xposed.utils.PayHelperUtils;

import de.robv.android.xposed.XposedBridge;

/**
 * Cordova插件通用广播接收器。
 * <p>
 * 当Hook代码执行操作时，会通过广播发送消息，此接收器在处理后通过Cordova插件回调给JS端。
 */
public class PluginBroadcastReceiver extends BroadcastReceiver {
  private static String TAG = PluginBroadcastReceiver.class.getSimpleName();

  private Activity mainActivity;

  @Override
  public void onReceive(final Context context, Intent intent) {
    try {
      String action = intent.getAction();
      switch (action) {
        // 支付宝用户Cookie获取成功
        case PluginIntentActions.AlipayCookieFetched: {
          String cookie = intent.getStringExtra("cookie");

          Toast toast = Toast.makeText(context, "Cookie接收成功", Toast.LENGTH_LONG);
          toast.show();

          XPosedPluginEntry.inst().setAlipayUserCookie(cookie);

          break;
        }

        // 用户信息获取成功
        case PluginIntentActions.AlipayUserInfoFetched: {
          String data = intent.getStringExtra("data");

          // Toast toast = Toast.makeText(context, "用户信息：" + data, Toast.LENGTH_LONG);
          // toast.show();

          XPosedPluginEntry.inst().setAlipayUserInfo(data);

          break;
        }

        // 支付宝订单消息
        case PluginIntentActions.AlipayBillReceived: {
          String no = intent.getStringExtra("bill_no");
          String money = intent.getStringExtra("bill_money");
          String mark = intent.getStringExtra("bill_mark");
          String type = intent.getStringExtra("bill_type");

          // todo: 发送给Cordova插件
          // collectUpCallback(type, no, money, mark, now);

          break;
        }

        // 支付宝二维码收款到账消息
        case PluginIntentActions.AppQrCodeReceived: {
          String money = intent.getStringExtra("money");
          String mark = intent.getStringExtra("mark");
          String type = intent.getStringExtra("type");

          // todo: 发送给Cordova插件

          break;
        }

        // 收到支付宝消息
        case PluginIntentActions.AlipayMessageReceived: {
          String msg = intent.getStringExtra("msg");

          // todo: 发送给Cordova插件

          break;
        }

        // 追加日志
        case PluginIntentActions.AppendLog: {
          String packageName = intent.getStringExtra("packageName");
          String content = intent.getStringExtra("content");
          String type = intent.getStringExtra("type");

          XPosedPluginEntry.inst().log(content, type);

          break;
        }

        // 商家服务，会收到一个订单号。通过URL获取这个订单的详细信息
        case PluginIntentActions.AlipayTradeNoFetched: {
          final String tradeNo = intent.getStringExtra("tradeNo");
          final String cookie = intent.getStringExtra("cookie");

          try {
              /*
              String orderUrl = "https://tradeeportlet.alipay.com/wireless/tradeDetail.htm?tradeNo=" + tradeNo + "&source=channel&_from_url=https%3A%2F%2Frender.alipay.com%2Fp%2Fz%2Fmerchant-mgnt%2Fsimple-order._h_t_m_l_%3Fsource%3Dmdb_card";

              HttpUtils httpUtils = new HttpUtils(15000);
              httpUtils.configResponseTextCharset("GBK");
              RequestParams params = new RequestParams();
              params.addHeader("Cookie", cookie);

              httpUtils.send(HttpRequest.HttpMethod.GET, orderUrl, params, new RequestCallBack<String>() {
                @Override
                public void onFailure(HttpException arg0, String arg1) {
                  PayHelperUtils.sendMsg(context, "支付宝服务器异常: " + arg1);
                }

                @Override
                public void onSuccess(ResponseInfo<String> arg0) {
                  try {
                    String result = arg0.result;
                    Document document = Jsoup.parse(result);
                    Elements elements = document.getElementsByClass("trade-info-value");
                    if (elements.size() >= 5) {
                      String money = document.getElementsByClass("amount").get(0).ownText().replace("+", "").replace("-", "");
                      String mark = elements.get(3).ownText();

                      // todo: 发送给Cordova插件
                    }
                  } catch (Exception e) {
                      // todo: 写入错误日志
                  }
                }
              });
              */
          } catch (Exception e) {
            // todo: 写入错误日志
          }

          break;
        }

        // 获取支付宝交易详情
        case PluginIntentActions.AlipayFetchTradeInfo: {
          new Handler().postDelayed(new Runnable() {
            public void run() {
              PayHelperUtils.getTradeInfo2(context);
            }
          }, 5 * 1000);
        }

        default: {
          break;
        }
      }
    } catch (Exception e) {
      Log.d(TAG, e.getMessage());

      Toast toast = new Toast(this.mainActivity);
      toast.setText(TAG + " 出错：" + e.getMessage());
      toast.setDuration(Toast.LENGTH_LONG);
      toast.show();
    }
  }

  /**
   * 设置主Activity。
   *
   * @param activity 主Activity。
   */
  public void setActivity(Activity activity) {
    this.mainActivity = activity;
  }

  /**
   * 收款回调。调用服务端。
   */
  public void collectUpCallback(String type, final String no, String money, String mark, String dt) {
    try {
      final String notifyUrl = AbSharedUtil.getString(mainActivity.getApplicationContext(), "callbackUrl");
      String signKey = AbSharedUtil.getString(mainActivity.getApplicationContext(), "signToken");

      if (TextUtils.isEmpty(notifyUrl) || TextUtils.isEmpty(signKey)) {
        // MainActivity.logTrace("发送异步通知异常，异步通知地址或密钥为空");
        // update(no, "异步通知地址或密钥为空");
        return;
      }

      String sign = MD5.digest(dt + mark + money + no + type + signKey + AbSharedUtil.getString(mainActivity.getApplicationContext(), "userids"));
      // MainActivity.logTrace("服务器针对（" + dt + mark + money + no + type + signKey + AbSharedUtil.getString(mainActivity.getApplicationContext(), "userids") + "）进行签名, 密钥是" + signKey + "。签名结果是：" + sign);

      RequestParams params = new RequestParams();
      params.addBodyParameter("type", type);
      params.addBodyParameter("no", no);
      params.addBodyParameter("userids", AbSharedUtil.getString(mainActivity.getApplicationContext(), "userids"));
      params.addBodyParameter("money", money);
      params.addBodyParameter("mark", mark);
      params.addBodyParameter("dt", dt);
      params.addBodyParameter("sign", sign);

      // 将外部App的用户ID一块传到服务器
      /*
      String outAppUserId = "";
      if (type.equals("alipay")) {
        // outAppUserId = AbSharedUtil.getString(getApplicationContext(), "alipay");
      } else if (type.equals("wechat")) {
        outAppUserId = AbSharedUtil.getString(mainActivity.getApplicationContext(), "wechat");
      } else if (type.equals("qq")) {
        outAppUserId = AbSharedUtil.getString(mainActivity.getApplicationContext(), "qq");
      }
      if (!TextUtils.isEmpty(outAppUserId)) {
        params.addBodyParameter("account", outAppUserId);
      }
      */

      XposedBridge.log("收款回调[POST]" + notifyUrl);

      /*
      HttpUtils httpUtils = new HttpUtils(30000);
      httpUtils.send(HttpRequest.HttpMethod.POST, notifyUrl, params, new RequestCallBack<String>() {
        @Override
        public void onFailure(HttpException arg0, String arg1) {
          XposedBridge.log("[回调失败]原因：" + arg1);
          // MainActivity.logTrace("发送异步通知(" + notifyUrl + ")异常，服务器异常" + arg1);

          update(no, arg1);
        }

        @Override
        public void onSuccess(ResponseInfo<String> arg0) {
          String result = arg0.result;
          XposedBridge.log("[回调成功]结果：" + result);

          // 回调成功后，向发送用户回复消息
          com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(result);
          String fromUserId = jsonObject.getString("user_id");
          String msg = jsonObject.getString("msg");
          AlipayHooker.inst().sendCollectUpMessageToPayee(fromUserId, "100", msg);

          update(no, result);
        }
      });
      */
    } catch (Exception e) {
      // MainActivity.logTrace("收款回调出错: " + e.getMessage());
    }
  }
}
