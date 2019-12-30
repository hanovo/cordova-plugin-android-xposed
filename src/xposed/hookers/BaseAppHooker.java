package com.skynet.xposed.hookers;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;


import com.skynet.xposed.hookers.alipay.AlipayBroadcastReceiver;

import java.util.List;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 钩子基类。
 */
public class BaseAppHooker {
  private static String TAG = BaseAppHooker.class.getSimpleName();

  @SuppressLint("StaticFieldLeak")
  public static Activity mAlipayLauncherActivity = null;

  protected static Context mAppContext = null;
  protected static ClassLoader mClassLoader = null;
  protected static AlipayBroadcastReceiver mAlipayBroadcastReceiver = null;
  protected static XC_LoadPackage.LoadPackageParam m_lpparam = null;

  /**
   * 判断是否已经Hook过App。
   */
  public boolean isHooked() {
    return mClassLoader != null;
  }

  /**
   * 启动app
   */
  public static void startApp() {
    try {
      /*Intent intent = new Intent(App.getInstance().getApplicationContext(), MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      App.getInstance().getApplicationContext().startActivity(intent);*/
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * 判断某一应用是否正在运行
   *
   * @param context     上下文
   * @param packageName 应用的包名
   * @return true 表示正在运行，false表示没有运行
   */
  public static boolean isAppRunning(Context context, String packageName) {
    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
    if (list.size() <= 0) {
      return false;
    }

    for (ActivityManager.RunningTaskInfo info : list) {
      if (info.baseActivity.getPackageName().equals(packageName)) {
        return true;
      }
    }

    return false;
  }

  /**
   * 获取通用的类对象。
   */
  protected Class<?> findClass(String className) {
    Class<?> ClassObject = XposedHelpers.findClass(className, mClassLoader);

    return ClassObject;
  }

  /**
   * 获取版本号名称
   */
  protected String getAppVersion(Context context) {
    String appVersion = "";
    try {
      appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException e) {
      XposedBridge.log(TAG + ": 获取App版本号失败。" + e.getMessage());
    }

    return appVersion;
  }

  /**
   * 获取当前本地apk的版本。获取软件版本号，对应AndroidManifest.xml下android:versionCode
   */
  public static int getVersionCode(Context mContext) {
    int versionCode = 0;

    try {
      versionCode = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      XposedBridge.log(TAG + ": 获取App版本代码失败。" + e.getMessage());
    }

    return versionCode;
  }
}
