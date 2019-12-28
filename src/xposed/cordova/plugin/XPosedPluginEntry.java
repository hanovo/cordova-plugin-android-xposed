package com.skynet.xposed.cordova.plugin;

import android.util.Log;

import com.skynet.xposed.hookers.HookableApps;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Cordova 插件入口。
 */
public class XPosedPluginEntry extends CordovaPlugin {
  private boolean isPaused;
  private static final String TAG = XPosedPluginEntry.class.getSimpleName();
  private static CallbackContext callbackContext;

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
   *
   * @param action          The action to execute.
   * @param args            The exec() arguments.
   * @param callbackContext The callback context used when calling back into JavaScript.
   * @return boolean
   * @throws JSONException JSON数据结构错误
   */
  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Log.i(TAG, "收到动作: " + action);

    switch (action) {
      case ActionListen:
        this.setCallbackContext(callbackContext);
        return true;

      case ActionGetHookableApps:
        this.getHookableApps();
        return true;

      case ActionHookApp:
        this.hookApp(args);
        return true;

      default:
        callbackContext.error(TAG + ". " + action + " is not a supported function.");
        return false;
    }
  }

  /**
   * 设置回调上下文。
   */
  private void setCallbackContext(CallbackContext cc) {
    Log.i(TAG, "Attaching callback context callbackContext " + cc);
    callbackContext = cc;

    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
    result.setKeepCallback(true);

    callbackContext.sendPluginResult(result);
  }

  /**
   * 获取可Hook的App列表。
   */
  private void getHookableApps() throws JSONException {
    HookableApps apps = HookableApps.inst();
    JSONArray json = new JSONArray(apps.getHookableApps());
    callbackContext.success(json);
  }

  /**
   * Hook App.
   */
  private void hookApp(JSONArray args) {

  }
}
