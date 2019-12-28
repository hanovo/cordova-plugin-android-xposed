package com.skynet.xposed.hookers;

import java.util.HashMap;
import java.util.Map;

/**
 * 可Hook的App列表。
 */
public class HookableApps {
  public static final String PackageAlipay = "com.eg.android.AlipayGphone";
  public static final String PackageUnionpay = "com.unionpay";
  public static final String PackageWechat = "com.tencent.mm";

  public static String ALARM_ACTION = "received.alarm.1";
  public static String ALIPAYSTART_ACTION = "received.alipay.start.1";
  public static String BILLRECEIVED_ACTION = "received.bill.1";
  public static String LOGINIDRECEIVED_ACTION = "received.loginid.1";
  public static String MSGRECEIVED_ACTION = "received.msg.1";
  public static String MYPACKAGE = "com.arya1021.alipay";
  public static String QRCODERECEIVED_ACTION = "received.qrcode.1";
  public static String TRADENORECEIVED_SHI_ACTION = "received.alipay.tradeno.1";
  public static String VOICE_ACTION = "received.action.voice";
  public static String WECHATSTART_ACTION = "received.wechat.start.1";

  private static Map<String, String> hookableApps = new HashMap<>();
  private static Map<String, String> hookedApps = new HashMap<>();

  private static HookableApps inst = null;

  /**
   * 初始化。
   */
  private HookableApps() {
    hookableApps.put(PackageAlipay, "支付宝");
    hookableApps.put(PackageUnionpay, "云闪付");
    hookableApps.put(PackageWechat, "微信");
  }

  /**
   * 单例。
   */
  public static HookableApps inst() {
    if (inst == null) {
      inst = new HookableApps();
    }

    return inst;
  }

  /**
   * 可Hook的App列表。
   */
  public Map<String, String> getHookableApps() {
    return hookableApps;
  }

  /**
   * 判断App是否可Hook。
   *
   * @param appId App Id
   */
  public boolean canHook(String appId) {
    return hookableApps.get(appId) != null;
  }

  /**
   * 已Hook的App列表。
   */
  public Map<String, String> getHookedApps() {
    return hookedApps;
  }

  /**
   * 已Hook的App。
   */
  public String getHookedApp(String appId) {
    return hookedApps.get(appId);
  }

  /**
   * App是否已经Hook。
   *
   * @param appId 要检测的AppId。
   */
  public boolean isHooked(String appId) {
    return hookedApps.get(appId) != null;
  }

  /**
   * 登记已Hook的App。
   *
   * @param appId 应用ID
   */
  public void addHookedApp(String appId) {
    if (this.isHooked(appId)) return;

    hookedApps.put(appId, appId);
  }
}
