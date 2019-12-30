package com.skynet.xposed.hookers.unionpay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.skynet.xposed.hookers.alipay.AlipayIntentActions;

import de.robv.android.xposed.XposedBridge;

/**
 * 云闪付操作广播接收器。此接收器需要注册到云闪付App。
 */
public class UnionpayActionReceiver extends BroadcastReceiver {
  private static String TAG = UnionpayActionReceiver.class.getSimpleName();

  /**
   * 处理接收到的事件。
   *
   * @param context 上下文
   * @param intent  数据负载
   */
  @Override
  public void onReceive(final Context context, Intent intent) {
    String action = intent.getAction();
    assert action != null;

    switch (action) {
      case AlipayIntentActions.LaunchCollectUp: {
        break;
      }

      default: {
        XposedBridge.log("未处理的云闪付操作事件");
        break;
      }
    }
  }
}
