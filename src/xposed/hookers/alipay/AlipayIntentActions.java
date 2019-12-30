package com.skynet.xposed.hookers.alipay;

/**
 * 支付宝App操作事件。
 */
public class AlipayIntentActions {
  // 发起主动收款
  public static final String LaunchCollectUp = "com.skynet.xposed.alipay.launch.collect.up";

  // 设置一些数据？
  public static final String SetData = "com.skynet.xposed.alipay.set.data";

  // 获取指定的交易详情
  public static final String GetTradeInfo = "com.skynet.xposed.alipay.get.trade.info";
}
