package com.skynet.xposed.hookers.wechat;

import android.content.Context;
import android.content.SharedPreferences;

import com.skynet.xposed.hookers.BaseAppHooker;
import com.skynet.xposed.utils.PayHelperUtils;

import de.robv.android.xposed.XposedHelpers;

/**
 * 微信钩子。
 */
public class WechatHooker extends BaseAppHooker {
  /**
   * 获取微信登录用户ID。
   */
  public String getWechatLoginId(Context context) {
    String loginId = "";

    try {
      SharedPreferences sharedPreferences = context.getSharedPreferences("com.tencent.mm_preferences", 0);
      loginId = sharedPreferences.getString("login_user_name", "");
    } catch (Exception e) {
      PayHelperUtils.sendMsg(context, e.getMessage());
    }

    return loginId;
  }

  /**
   * 获取微信余额。
   */
  public double getWechatBalance(ClassLoader classLoader) {
    double balance = 0.0;

    Class<?> p = this.findClass("com.tencent.mm.plugin.wallet.a.p");
    XposedHelpers.callStaticMethod(p, "bNp");
    Object ag = XposedHelpers.callStaticMethod(p, "bNq");
    Object paw = XposedHelpers.getObjectField(ag, "paw");

    if (paw != null) {
      balance = (Double) XposedHelpers.getObjectField(paw, "plV");
    }

    return balance;
  }
}
