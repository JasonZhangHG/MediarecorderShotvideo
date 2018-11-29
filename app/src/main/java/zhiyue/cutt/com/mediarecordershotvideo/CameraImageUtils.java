package zhiyue.cutt.com.mediarecordershotvideo;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CameraImageUtils {

    public static String takePhoto(byte[] yuv, int width, int height) {
        Rect rect = new Rect(0, 0, width, height);
        YuvImage img = new YuvImage(yuv, ImageFormat.NV21, width, height, null);
        OutputStream outStream = null;
        checkParentDir();
        File file;
        checkJpegDir();
        file = createJpeg();
        try {
            outStream = new FileOutputStream(file);
            img.compressToJpeg(rect, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //new Thread(new ImageSaver(reader2)).start();
        return file.toString();
    }

    /**
     * 判断父文件是否存在
     */
    private static void checkParentDir() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Holla/");
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * 判断文件夹是否存在
     */
    private static void checkJpegDir() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Holla/camera/");
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * 创建jpeg的文件
     *
     * @return
     */
    private static File createJpeg() {
        long time = System.currentTimeMillis();
        File dir = new File(Environment.getExternalStorageDirectory() + "/Holla/camera/");
        return new File(dir, time + ".jpg");
    }


    public static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }
}
