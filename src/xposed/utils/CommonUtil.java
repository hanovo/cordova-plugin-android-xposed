package com.skynet.xposed.utils;

import android.os.Environment;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 配置工具。
 */
public class CommonUtil {
  private static final String FILEPATH = "/xpLog.txt";
  private static final String CONFIGFILEPATH = Environment.getExternalStorageDirectory() + "/alipay/data";
  private static boolean debug = false;

  /**
   * 读文
   */
  public static String readFileSdcard() {
    File file = new File(Environment.getExternalStorageDirectory() + FILEPATH);
    String res = "";
    try {
      FileInputStream fin = new FileInputStream(file);
      int length = fin.available();
      byte[] buffer = new byte[length];
      fin.read(buffer);
      res = new String(buffer, "UTF-8");
      fin.close();
    } catch (Exception e) {
      CommonUtil.log(e.getMessage() + e.getCause());
    }

    return res;
  }

  public static void writeStringtoFile(String str) {
    File file = new File(Environment.getExternalStorageDirectory() + FILEPATH);
    File file2 = new File("/root" + FILEPATH);
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    try {
      FileOutputStream ops = new FileOutputStream(file, false);
      ops.write(str.getBytes());
      ops.flush();
      ops.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void writeConfigtoFile(String name, String value) {
    try {
      File file = new File(CONFIGFILEPATH + File.separator + name + ".con");
      if (!file.exists()) {
        file.getParentFile().mkdirs();
        file.createNewFile();
      }
      FileOutputStream ops = new FileOutputStream(file, false);
      ops.write(value.getBytes());
      ops.flush();
      ops.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      CommonUtil.log(e.getMessage());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      CommonUtil.log(e.getMessage());
    }
  }

  public static String readConfigFromFile(String name, String defaul) {
    try {
      File file = new File(CONFIGFILEPATH + File.separator + name + ".con");
      if (!file.exists()) {
        return defaul;
      }
      FileInputStream fin = new FileInputStream(file);
      int length = fin.available();
      byte[] buffer = new byte[length];
      fin.read(buffer);
      defaul = new String(buffer, "UTF-8");
      fin.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      CommonUtil.log(e.getMessage());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      CommonUtil.log(e.getMessage());
    }
    return defaul;
  }


  public static String formatDateForName() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmsss");
    return dateFormat.format(new Date());
  }

  public static void copyFolder(File newFile, File oldFile) throws Exception {

    FileOutputStream fos = new FileOutputStream(newFile);
    FileInputStream fis = new FileInputStream(oldFile);
    byte[] buff = new byte[100 * 1024];
    int length = 0;
    while ((length = fis.read(buff)) != -1) {
      fos.write(buff, 0, length);
      fos.flush();
    }
    fis.close();
    buff = null;
    fos.close();
  }

  public static String getBase64String(File file) {
    try {
      FileInputStream inStream = new FileInputStream(file);
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int len = 0;
      while ((len = inStream.read(buffer)) != -1) {
        outStream.write(buffer, 0, len);
      }
      outStream.close();
      inStream.close();
      buffer = outStream.toByteArray();
      return new String(Base64.encode(buffer, Base64.DEFAULT), "utf-8");
    } catch (IOException e) {

    }
    return null;
  }

  public static void log(Throwable error) {
  }

  public static void log(String error) {
  }
}

