package com.skynet.xposed.cordova.plugin;

/**
 * 支付宝数据传输操作。
 */
public class PluginIntentActions {
  // 支付宝用户信息获取成功
  public static final String AlipayUserInfoFetched = "com.skynet.xposed.alipay.user.info.fetched";

  // 支付宝用户社交信息获取成功
  public static final String AlipaySocialInfoFetched = "com.skynet.xposed.alipay.user.social.info.fetched";

  // 支付宝用户Cookie获取成功
  public static final String AlipayCookieFetched = "com.skynet.xposed.alipay.cookie.fetched";

  // 支付宝收款消息
  public static final String AlipayBillReceived = "com.skynet.xposed.alipay.bill.received";

  // 支付宝普通消息
  public static final String AlipayMessageReceived = "com.skynet.xposed.alipay.message.received";

  // 获取交易记录成功
  public static final String AlipayFetchTradeInfo = "com.skynet.xposed.alipay.fetch.trade.info";

  // 收到商户服务消息
  public static final String AlipayTradeNoFetched = "com.skynet.xposed.alipay.trade.number.fetched";

  // 追加日志
  public static final String AppendLog = "com.skynet.xposed.append.log";

  public static final String AppQrCodeReceived = "com.tools.payhelper.qrcodereceived";
  public static final String Notify = "com.tools.payhelper.notify";
  public static final String UpdateUserBalance = "com.tools.payhelper.updatebalance";
  public static final String AlipayLaunch = "com.payhelper.alipay.launchApp";

}
