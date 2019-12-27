package com.skynet.xposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {
  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
    if (!lpparam.packageName.equals("com.example.basic")) return;

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
  }
}
