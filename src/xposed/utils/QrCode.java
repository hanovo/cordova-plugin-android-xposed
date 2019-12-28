package com.skynet.xposed.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static android.graphics.Color.BLACK;

/**
 * 二维码工具。
 */
public class QrCode {
  /**
   * 解析二维码图片
   */
  public static Result extra(String path) {
    Bitmap scanBitmap;
    if (TextUtils.isEmpty(path)) return null;

    Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
    hints.put(DecodeHintType.CHARACTER_SET, "UTF-8"); // 设置二维码内容的编码

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true; // 先获取原大小

    // scanBitmap = BitmapFactory.decodeFile(path,options);
    scanBitmap = ImageUtils.getBitmapByFile(path);
    options.inJustDecodeBounds = false;
    int sampleSize = (int) (options.outHeight / (float) 200);
    if (sampleSize <= 0) sampleSize = 1;

    options.inSampleSize = sampleSize;

    scanBitmap = BitmapFactory.decodeFile(path, options);
    int[] data = new int[scanBitmap.getWidth() * scanBitmap.getHeight()];
    scanBitmap.getPixels(data, 0, scanBitmap.getWidth(), 0, 0, scanBitmap.getWidth(), scanBitmap.getHeight());

    RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(scanBitmap.getWidth(), scanBitmap.getHeight(), data);
    BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(rgbLuminanceSource));
    QRCodeReader reader = new QRCodeReader();
    Result result = null;

    try {
      result = reader.decode(binaryBitmap, hints);
    } catch (NotFoundException e) {
      Log.e("hxy", "NotFoundException");
    } catch (ChecksumException e) {
      Log.e("hxy", "ChecksumException");
    } catch (FormatException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return result;
  }

  /**
   * 生成二维码图片（不带图片）
   */
  public static Bitmap createQRCode(String url, int widthAndHeight)
      throws WriterException {
    Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
    hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
    BitMatrix matrix = new MultiFormatWriter().encode("lvu",
        BarcodeFormat.QR_CODE, widthAndHeight, widthAndHeight);

    int width = matrix.getWidth();
    int height = matrix.getHeight();
    int[] pixels = new int[width * height];
    //画黑点
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (matrix.get(x, y)) {
          pixels[y * width + x] = BLACK; //0xff000000
        }
      }
    }
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }

  /**
   * 带图片的二维码
   */
  public static Bitmap createQRImage(String content, int heightPix, Bitmap logoBm) {
    try {
      //配置参数
      Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
      hints.put(EncodeHintType.CHARACTER_SET, "utf-8");

      //容错级别
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

      // 图像数据转换，使用了矩阵转换
      BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, heightPix, heightPix, hints);
      int[] pixels = new int[heightPix * heightPix];

      // 下面这里按照二维码的算法，逐个生成二维码的图片，
      // 两个for循环是图片横列扫描的结果
      for (int y = 0; y < heightPix; y++) {
        for (int x = 0; x < heightPix; x++) {
          if (bitMatrix.get(x, y)) {
            pixels[y * heightPix + x] = 0xff000000;
          } else {
            pixels[y * heightPix + x] = 0xffffffff;
          }
        }
      }

      // 生成二维码图片的格式，使用ARGB_8888
      Bitmap bitmap = Bitmap.createBitmap(heightPix, heightPix, Bitmap.Config.ARGB_8888);
      bitmap.setPixels(pixels, 0, heightPix, 0, 0, heightPix, heightPix);

      if (logoBm != null) bitmap = addLogo(bitmap, logoBm);

      //必须使用compress方法将bitmap保存到文件中再进行读取。直接返回的bitmap是没有任何压缩的，内存消耗巨大！
      return bitmap;
    } catch (WriterException e) {
      e.printStackTrace();
    }

    return null;
  }

  @SuppressLint("NewApi")
  public static String bitmapToBase64(Bitmap bitmap) {
    String reslut = null;
    ByteArrayOutputStream baos = null;

    try {
      if (bitmap != null) {
        baos = new ByteArrayOutputStream();

        // 压缩只对保存有效果bitmap还是原来的大小
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);

        baos.flush();
        baos.close();
        // 转换为字节数组
        byte[] byteArray = baos.toByteArray();

        // 转换为字符串
        reslut = Base64.encodeToString(byteArray, Base64.DEFAULT);
      } else {
        return null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (baos != null) {
          baos.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return reslut;
  }

  /**
   * 在二维码中间添加Logo图案
   */
  private static Bitmap addLogo(Bitmap src, Bitmap logo) {
    if (src == null) return null;
    if (logo == null) return src;

    //获取图片的宽高
    int srcWidth = src.getWidth();
    int srcHeight = src.getHeight();
    int logoWidth = logo.getWidth();
    int logoHeight = logo.getHeight();

    if (srcWidth == 0 || srcHeight == 0) return null;
    if (logoWidth == 0 || logoHeight == 0) return src;

    // logo大小为二维码整体大小的1/5
    float scaleFactor = srcWidth * 1.0f / 5 / logoWidth;
    Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
    try {
      Canvas canvas = new Canvas(bitmap);
      canvas.drawBitmap(src, 0, 0, null);
      canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
      canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);

      canvas.save();
      canvas.restore();
    } catch (Exception e) {
      bitmap = null;
      e.getStackTrace();
    }

    return bitmap;
  }
}
