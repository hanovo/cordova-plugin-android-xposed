package com.skynet.xposed.hookers.alipay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import com.lidroid.xutils.http.RequestParams;
import com.skynet.xposed.utils.AbSharedUtil;
import com.skynet.xposed.utils.MD5;
import com.skynet.xposed.utils.PayHelperUtils;
import com.skynet.xposed.beans.QrCodeBean;

import java.text.DecimalFormat;
import java.util.Objects;

import de.robv.android.xposed.XposedBridge;

/**
 * 通用广播接收器。
 */
public class CommonActionReceiver extends BroadcastReceiver {
  private static String TAG = CommonActionReceiver.class.getSimpleName();

  private Activity mActivity;

  public void setActivity(Activity activity) {
    this.mActivity = activity;
  }

  @Override
  public void onReceive(final Context context, Intent intent) {
    try {
      String action = intent.getAction();
      switch (Objects.requireNonNull(action)) {
        case AlipayIntentActions.AppBillReceived: {
          PayHelperUtils.sendMsg(context, "get Post--");

          String no = intent.getStringExtra("bill_no");
          String money = intent.getStringExtra("bill_money");
          String mark = intent.getStringExtra("bill_mark");
          String type = intent.getStringExtra("bill_type");
          // MainActivity.logTrace("收到" + type + "订单,订单号：" + no + "金额：" + money + "备注：" + mark);

          String channel = "";
          String now = System.currentTimeMillis() + "";

          if (type.equals("alipay")) {
            channel = "支付宝";
          } else if (type.equals("alipay_dy")) {
            channel = "支付宝店员";
            now = intent.getStringExtra("time");
          } else if (type.equals("wechat")) {
            channel = "微信支付";
          } else if (type.equals("qq")) {
            channel = "QQ钱包";
          } else if (type.equals("unionpay")) {
            channel = "云闪付";
          } else if (type.equals("abcpay")) {
            channel = "农商银行";
          }

          // MainActivity.logTrace(">>>收到" + channel + "订单, 订单号：" + no + "，金额：" + money + "，备注：" + mark);
          collectUpCallback(type, no, money, mark, now);

          break;
        }

        case AlipayIntentActions.AppQrCodeReceived: {
          String money = intent.getStringExtra("money");
          String mark = intent.getStringExtra("mark");
          String type = intent.getStringExtra("type");
          String payUrl = intent.getStringExtra("payurl");

          AlipayDataCache dbManager = new AlipayDataCache(context);
          String dt = System.currentTimeMillis() + "";
          DecimalFormat df = new DecimalFormat("0.00");
          money = df.format(Double.parseDouble(money));
          dbManager.addQrCode(new QrCodeBean(money, mark, type, payUrl, dt));

          // MainActivity.logTrace("生成成功, 金额:" + money + "备注:" + mark + "二维码:" + payUrl);
          break;
        }

        case AlipayIntentActions.AppMessageReceived: {
          String msg = intent.getStringExtra("msg");
          // MainActivity.logTrace(msg);
          break;
        }

        case AlipayIntentActions.AlipaySaveCookie: {
          String cookie = intent.getStringExtra("alipaycookie");
          PayHelperUtils.updateAlipayCookie(mActivity, cookie);
          break;
        }

        case AlipayIntentActions.AppLoginIdReceived: {
          String type = intent.getStringExtra("type");
          String loginid = intent.getStringExtra("loginid");
          if (TextUtils.isEmpty(loginid)) break;

          String log;
          switch (type) {
            case "wechat": {
              log = "微信号：" + loginid;
              break;
            }

            case "alipay": {
              log = "支付宝号：" + loginid;
              break;
            }

            case "qq": {
              log = "登QQ号：" + loginid;
              break;
            }

            default: {
              log = "未知登录ID：" + loginid;
              break;
            }
          }

          // MainActivity.logTrace(log);
          AbSharedUtil.putString(mActivity.getApplicationContext(), type, loginid);

          break;
        }

        case AlipayIntentActions.AppTradeNoReceived: {
          // 商家服务
          final String tradeno = intent.getStringExtra("tradeno");
          String cookie = intent.getStringExtra("cookie");

          final AlipayDataCache dbManager = new AlipayDataCache(context);
          if (!dbManager.isExistTradeNo(tradeno)) {
            dbManager.addTradeNo(tradeno, "0");
            String url = "https://tradeeportlet.alipay.com/wireless/tradeDetail.htm?tradeNo=" + tradeno + "&source=channel&_from_url=https%3A%2F%2Frender.alipay.com%2Fp%2Fz%2Fmerchant-mgnt%2Fsimple-order._h_t_m_l_%3Fsource%3Dmdb_card";

            try {
              /*
              HttpUtils httpUtils = new HttpUtils(15000);
              httpUtils.configResponseTextCharset("GBK");
              RequestParams params = new RequestParams();
              params.addHeader("Cookie", cookie);

              httpUtils.send(HttpRequest.HttpMethod.GET, url, params, new RequestCallBack<String>() {
                @Override
                public void onFailure(HttpException arg0, String arg1) {
                  PayHelperUtils.sendMsg(context, "服务器异常" + arg1);
                }

                @Override
                public void onSuccess(ResponseInfo<String> arg0) {
                  try {
                    String result = arg0.result;
                    Document document = Jsoup.parse(result);
                    Elements elements = document.getElementsByClass("trade-info-value");
                    if (elements.size() >= 5) {
                      dbManager.updateTradeNo(tradeno, "1");
                      String money = document.getElementsByClass("amount").get(0).ownText().replace("+", "").replace("-", "");
                      String mark = elements.get(3).ownText();
                      String dt = System.currentTimeMillis() + "";
                      dbManager.addOrder(new OrderBean(money, mark, "alipay", tradeno, dt, "", 0));
                      MainActivity.logTrace("收到支付宝订单,订单号：" + tradeno + "金额：" + money + "备注：" + mark);
                      collectUpCallback("alipay", tradeno, money, mark, dt);
                    }
                  } catch (Exception e) {
                    PayHelperUtils.sendMsg(context, "AlipayIntentActions.AppTradeNoReceived-->>onSuccess异常" + e.getMessage());
                  }
                }
              });
              */
            } catch (Exception e) {
              PayHelperUtils.sendMsg(context, "AlipayIntentActions.TRADE_NO_RECEIVED异常" + e.getMessage());
            }
          } else {
            // MainActivity.logTrace("出现重复流水号，疑似掉单，5秒后自动补单");
            new Handler().postDelayed(new Runnable() {
              public void run() {
                PayHelperUtils.getTradeInfo2(context);
              }
            }, 5 * 1000);
          }
          break;
        }

        case AlipayIntentActions.GetTradeInfo: {
          new Handler().postDelayed(new Runnable() {
            public void run() {
              PayHelperUtils.getTradeInfo2(context);
            }
          }, 5 * 1000);
        }
      }
    } catch (Exception e) {
      PayHelperUtils.sendMsg(context, "BillReceived异常" + e.getMessage());
    }
  }

  /**
   * 收款回调。调用服务端。
   */
  public void collectUpCallback(String type, final String no, String money, String mark, String dt) {
    try {
      final String notifyUrl = AbSharedUtil.getString(mActivity.getApplicationContext(), "callbackUrl");
      String signKey = AbSharedUtil.getString(mActivity.getApplicationContext(), "signToken");

      if (TextUtils.isEmpty(notifyUrl) || TextUtils.isEmpty(signKey)) {
        // MainActivity.logTrace("发送异步通知异常，异步通知地址或密钥为空");
        // update(no, "异步通知地址或密钥为空");
        return;
      }

      String sign = MD5.digest(dt + mark + money + no + type + signKey + AbSharedUtil.getString(mActivity.getApplicationContext(), "userids"));
      // MainActivity.logTrace("服务器针对（" + dt + mark + money + no + type + signKey + AbSharedUtil.getString(mActivity.getApplicationContext(), "userids") + "）进行签名, 密钥是" + signKey + "。签名结果是：" + sign);

      RequestParams params = new RequestParams();
      params.addBodyParameter("type", type);
      params.addBodyParameter("no", no);
      params.addBodyParameter("userids", AbSharedUtil.getString(mActivity.getApplicationContext(), "userids"));
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
        outAppUserId = AbSharedUtil.getString(mActivity.getApplicationContext(), "wechat");
      } else if (type.equals("qq")) {
        outAppUserId = AbSharedUtil.getString(mActivity.getApplicationContext(), "qq");
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

  // 更新本地订单状态
  private void update(String no, String result) {
    AlipayDataCache dbManager = new AlipayDataCache(null);
    dbManager.updateOrder(no, result);
  }
}
