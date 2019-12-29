package com.skynet.xposed.hookers.alipay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 支付宝广播接收器。
 */
public class AlipayReceiver extends BroadcastReceiver {
  private static String TAG = AlipayReceiver.class.getSimpleName();

  /**
   * 处理接收到的事件。
   *
   * @param context 上下文
   * @param intent  数据负载
   */
  @Override
  public void onReceive(final Context context, Intent intent) {
    XposedBridge.log(TAG + ": 支付宝启动");

    switch (intent.getAction()) {
      case AlipayIntentActions.AlipayLaunchCollectUp: {
        String mark = intent.getStringExtra("mark");
        final String money = intent.getStringExtra("money");
        if (mark.contains("---")) {
          String[] list = mark.split("---");
          final String remark = list[0];
          final String uid = list[1];
          new Thread(new Runnable() {
            @Override
            public void run() {
              final String res = AlipayHooker.inst().sendCollectUpMessageToPayee(uid, money, remark);
              if (context instanceof Activity) {
                Activity activity = (Activity) context;
                activity.runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    Intent broadCastIntent = new Intent();
                    broadCastIntent.putExtra("money", money + "");
                    broadCastIntent.putExtra("mark", remark + "---" + uid);
                    broadCastIntent.putExtra("type", "alipay");
                    broadCastIntent.putExtra("payurl", res);
                    broadCastIntent.setAction(AlipayIntentActions.AppQrCodeReceived);
                    context.sendBroadcast(broadCastIntent);
                  }
                });
              }
            }
          }).start();
        } else {
          Intent intent2 = new Intent(context, XposedHelpers.findClass("com.alipay.mobile.payee.ui.PayeeQRSetMoneyActivity", context.getClassLoader()));
          intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent2.putExtra("mark", mark);
          intent2.putExtra("money", money);
          context.startActivity(intent2);
        }

        break;
      }

      default: {
        XposedBridge.log("111");

        break;
      }
    }
  }
}
