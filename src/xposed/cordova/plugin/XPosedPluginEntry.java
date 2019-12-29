package com.skynet.xposed.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.skynet.xposed.hookers.HookableApps;
import com.skynet.xposed.hookers.alipay.AlipayHooker;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Cordova 插件入口。
 */
public class XPosedPluginEntry extends CordovaPlugin {
  private boolean isPaused;
  private CallbackContext callbackContext;
  private static final String TAG = XPosedPluginEntry.class.getSimpleName();

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
   * 通知JS。
   *
   * @param jsonObject JSON 数据结构。
   */
  public void notifyJavascript(Object jsonObject) {
    this.callbackContext.success(JSON.toJSONString(jsonObject));
  }

  /**
   * 初始化插件。初始化之后，会保存该插件的回调上下文，以便JS接收事件通知。
   */
  protected void init(JSONArray args, CallbackContext cc) {
    callbackContext = cc;
    callbackContext.success("XPosedPluginEntry 插件初始化成功");
  }

  /**
   * 获取可Hook的App列表。
   */
  protected void getHookableApps(CallbackContext callbackContext) throws JSONException {
    try {
      HookableApps apps = HookableApps.inst();
      JSONArray json = new JSONArray(apps.getHookableApps());
      callbackContext.success(json);
    } catch (Exception e) {
      JSONObject err = new JSONObject();
      err.put("message", e.getMessage());

      callbackContext.error(err);
    }
  }

  /**
   * Start App.
   */
  protected void startApp(JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      String packageName = args.get(0).toString();
      if (packageName.equals(HookableApps.PackageAlipay) && !AlipayHooker.inst().isHooked()) {
        callbackContext.error("当前非XPosed环境或未启用XPosed插件，无法启动App");
        return;
      }

      Context context = webView.getContext();
      Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
      if (intent == null) return;

      intent.putExtra("type", "110");
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);

      HookableApps.inst().statHookedAppListening(packageName);

      callbackContext.success(1);
    } catch (Exception e) {
      JSONObject err = new JSONObject();
      err.put("message", e.getMessage());

      callbackContext.error(err);
    }
  }

  /**
   * Stop App.
   */
  protected void stopApp(JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      String packageName = args.get(0).toString();
      HookableApps.inst().stopHookedAppListening(packageName);

      callbackContext.success(1);
    } catch (Exception e) {
      JSONObject err = new JSONObject();
      err.put("message", e.getMessage());

      callbackContext.error(err);
    }
  }

  /**
   * 获取用户信息。
   */
  protected void getLoginUserInfo(JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      String packageName = args.get(0).toString();
      if (!HookableApps.inst().isHooked(packageName)) {
        throw new Exception("当前非XPosed环境或未启用XPosed插件，App尚未Hook");
      }

      if (packageName.equals(HookableApps.PackageAlipay)) {
        Map<String, Object> userInfo = AlipayHooker.inst().getLoginUserInfo();

        callbackContext.success(JSON.toJSONString(userInfo));
      } else {
        throw new Exception("不支持的App: " + packageName);
      }
    } catch (Exception e) {
      JSONObject err = new JSONObject();
      err.put("message", e.getMessage());

      callbackContext.error(err);
    }
  }
}
