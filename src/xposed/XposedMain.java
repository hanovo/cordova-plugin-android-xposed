package com.skynet.xposed;

import android.content.pm.ApplicationInfo;

import com.skynet.xposed.hookers.HookableApps;
import com.skynet.xposed.hookers.alipay.AlipayHooker;
import com.skynet.xposed.hookers.wechat.WechatHooker;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * XPosed入口
 */
public class XposedMain implements IXposedHookLoadPackage {
  private static boolean isAlipayHooked = false;
  private static boolean isUnionpayHooked = false;
  private static boolean isWechatHooked = false;
  
  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
    // 过滤系统应用
    if (param.appInfo == null || (param.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM |
        ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
      return;
    }

    // 忽略不可Hook的App
    if (!HookableApps.inst().canHook(param.packageName)) return;

    switch (param.packageName) {
      case HookableApps.PackageAlipay:
        if (!isAlipayHooked) {
          isAlipayHooked = true;
          AlipayHooker.inst().hook(param);
        } else {
          XposedBridge.log("支付宝已经Hook过了...");
        }
        break;

      case HookableApps.PackageUnionpay:
        if (!isUnionpayHooked) {
          isUnionpayHooked = true;
        } else {
          XposedBridge.log("云闪付已经Hook过了...");
        }
        break;

      case HookableApps.PackageWechat:
        if (!isWechatHooked) {
          isWechatHooked = true;
          WechatHooker.inst().hook(param);
        } else {
          XposedBridge.log("微信已经Hook过了...");
        }
        break;

      default:
        break;
    }
  }
}
