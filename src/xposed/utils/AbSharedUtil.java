package com.skynet.xposed.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 保存到 SharedPreferences 的数据.
 */
public class AbSharedUtil {
  private static final String SHARED_PATH = "bbplayer";

  public static SharedPreferences getDefaultSharedPreferences(Context context) {
    return context.getSharedPreferences(SHARED_PATH, Context.MODE_PRIVATE);
  }

  public static void putInt(Context context, String key, int value) {
    CommonUtil.writeConfigtoFile(key, "" + value);
  }

  public static int getInt(Context context, String key) {
    String re = CommonUtil.readConfigFromFile(key, "0");

    return Integer.valueOf(re);
  }

  public static void putString(Context context, String key, String value) {
    CommonUtil.writeConfigtoFile(key, "" + value);
  }

  public static String getString(Context context, String key) {
    String re = CommonUtil.readConfigFromFile(key, "");
    return String.valueOf(re);
  }

  public static void putBoolean(Context context, String key, boolean value) {
    CommonUtil.writeConfigtoFile(key, "" + value);
  }

  public static boolean getBoolean(Context context, String key, boolean defValue) {
    String re = CommonUtil.readConfigFromFile(key, "0");

    return Boolean.valueOf(re);
  }
}
