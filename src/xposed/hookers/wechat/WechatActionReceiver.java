package com.skynet.xposed.hookers.wechat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.skynet.xposed.hookers.alipay.AlipayIntentActions;

import java.util.Objects;

import de.robv.android.xposed.XposedBridge;

/**
 * 微信操作广播接收器。此接收器需要注册到向信
 */
public class WechatActionReceiver extends BroadcastReceiver {
  private static String TAG = WechatActionReceiver.class.getSimpleName();

  /**
   * 处理接收到的事件。
   *
   * @param context 上下文
   * @param intent  数据负载
   */
  @Override
  public void onReceive(final Context context, Intent intent) {
    switch (Objects.requireNonNull(intent.getAction())) {
      case AlipayIntentActions.LaunchCollectUp: {
        break;
      }

      default: {
        XposedBridge.log("未处理的微信操作事件");
        break;
      }
    }
  }
}
