package com.skynet.xposed.utils;

public class StringUtils {
  public static String getTextCenter(String text, String begin, String end) {
    try {
      int b = text.indexOf(begin) + begin.length();
      int e = text.indexOf(end, b);
      return text.substring(b, e);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return "error";
    }
  }
}