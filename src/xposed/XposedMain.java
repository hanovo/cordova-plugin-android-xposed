package com.skynet.xposed;

import android.content.pm.ApplicationInfo;

import com.skynet.xposed.hookers.HookableApps;
import com.skynet.xposed.hookers.alipay.AlipayHooker;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * XPosed入口
 */
public class XposedMain implements IXposedHookLoadPackage {
  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
    // 过滤系统应用
    if (lpparam.appInfo == null || (lpparam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
      return;
    }

    // 忽略不可Hook的App
    if (!HookableApps.inst().canHook(lpparam.packageName)) return;

    switch (lpparam.packageName) {
      case HookableApps.PackageAlipay:
        AlipayHooker.inst().hook(lpparam);
        break;

      case HookableApps.PackageUnionpay:
        break;

      case HookableApps.PackageWechat:
        break;

      default:
        break;
    }

    /*
    XposedHelpers.findAndHookMethod("com.example.basic.MainActivity", lpparam.classLoader, "createNewTitle", String.class, new XC_MethodHook() {
      @Override
      protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        XposedBridge.log("param is: " + param.args[0]);
        param.args[0] = "Xposed"; // 修改替换第1个参数
        // param.setResult(null); // 拦截方法，不给处理
      }

      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Object result = param.getResult(); // 返回值
        XposedBridge.log("result is: " + result);
        // param.setResult("Hello Xposed!"); // 修改替换返回值
      }
    });
    */
  }
}
