package com.skynet.xposed.hookers.qq;

import android.content.Context;
import android.content.SharedPreferences;

import com.skynet.xposed.hookers.BaseAppHooker;
import com.skynet.xposed.utils.PayHelperUtils;

/**
 * QQ 钩子。
 */
public class QQHooker extends BaseAppHooker {
  public static String getQQLoginId(Context context) {
    String loginId = "";
    try {
      SharedPreferences sharedPreferences = context.getSharedPreferences("Last_Login", 0);
      loginId = sharedPreferences.getString("uin", "");
    } catch (Exception e) {
      PayHelperUtils.sendMsg(context, e.getMessage());
    }
    return loginId;
  }
}
