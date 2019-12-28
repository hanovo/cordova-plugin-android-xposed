package com.skynet.xposed.cordova.plugin;

import android.util.Log;

import com.skynet.xposed.hookers.HookableApps;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Method;

/**
 * Cordova 插件入口。
 */
public class XPosedPluginEntry extends CordovaPlugin {
  private boolean isPaused;
  private static final String TAG = XPosedPluginEntry.class.getSimpleName();

  // 操作列表
  private static final String ActionGetHookableApps = "getHookableApps";
  private static final String ActionListen = "listen";
  private static final String ActionHookApp = "hookableApp";

  @Override
  public void onPause(boolean multitasking) {
    this.isPaused = true;
  }

  @Override
  public void onResume(boolean multitasking) {
    this.isPaused = false;
  }

  public boolean isPaused() {
    return isPaused;
  }

  /**
   * 插件与JS交互入口。
   */
  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Log.i(TAG, "收到动作: " + action);

    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        try {
          Method method = XPosedPluginEntry.class.getDeclaredMethod(action, JSONArray.class, CallbackContext.class);
          method.invoke(XPosedPluginEntry.this, args, callbackContext);
        } catch (Exception e) {
          Log.e(TAG, e.toString());
        }
      }
    });
    return true;
  }

  /**
   * 初始化插件。
   */
  void init(JSONArray args, CallbackContext callbackContext) {
    // todo: 初始化
  }

  /**
   * 获取可Hook的App列表。
   */
  void getHookableApps(CallbackContext callbackContext) {
    try {
      HookableApps apps = HookableApps.inst();
      JSONArray json = new JSONArray(apps.getHookableApps());
      callbackContext.success(json);
    } catch (Exception e) {
      callbackContext.error("Parameters error. " + e);
    }
  }

  /**
   * Hook App.
   */
  void hookApp(JSONArray args, CallbackContext callbackContext) {
    try {
      String packageName = args.get(0).toString();
    } catch (Exception e) {
      callbackContext.error("Parameters error. " + e);
    }
  }
}
