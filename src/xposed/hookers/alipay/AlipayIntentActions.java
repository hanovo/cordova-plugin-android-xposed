package com.skynet.xposed.hookers.alipay;

/**
 * 支付宝数据传输操作。
 */
public class AlipayIntentActions {
  public static final String AppBillReceived = "com.skynet.xposed.alipay.billreceived";

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
}
