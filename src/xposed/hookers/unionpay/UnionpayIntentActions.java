package com.skynet.xposed.hookers.unionpay;

/**
 * 云闪付数据传输操作。
 */
public class UnionpayIntentActions {
  public static final String AppBillReceived = "com.tools.payhelper.billreceived";
  public static final String AppQrCodeReceived = "com.tools.payhelper.qrcodereceived";
  public static final String AppMessageReceived = "com.tools.payhelper.msgreceived";
  public static final String AppTradeNoReceived = "com.tools.payhelper.tradenoreceived";
  public static final String AppLoginIdReceived = "com.tools.payhelper.loginidreceived";
  public static final String Notify = "com.tools.payhelper.notify";
  public static final String UpdateUserBalance = "com.tools.payhelper.updatebalance";

  public static final String GetTradeInfo = "com.tools.payhelper.gettradeinfo";

  public static final String AlipayLaunch = "com.payhelper.alipay.launchApp";
  public static final String AlipayLaunchCollectUp = "com.payhelper.alipay.launchAppToCollectUp";
  public static final String AlipaySaveCookie = "com.tools.payhelper.savealipaycookie";
  public static final String AlipaySetData = "com.payhelper.alipay.setData";

  public static final String UnionPayLaunch = "com.colin.union.start";

  public static final String QQLaunch = "com.payhelper.qq.start";

  public static final String WechatLaunch = "com.payhelper.wechat.start";
}
