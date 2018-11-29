package zhiyue.cutt.com.mediarecordershotvideo;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class StorageUtil {

    private static final String TEMP_FOLDER = "temp";
    private static final String VIDEO_FOLDER = "video";
    private static final String RAW_FOLDER = "raw";
    private static final String RAW_CACHE_FOLDER = "raw_cache";
    private static final String CAMERA_VIDEO_FOLDER = "camera_video_folder";
    private static final String COVER_PAGE_FOLDER = "cover_page_folder";
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM/monkey" + File.separator;
    private static final File parentPath = Environment.getExternalStorageDirectory();
    private static String storagePath = "";

    public static String saveFile(byte[] data, String dir, String fileName) {
        BufferedOutputStream bos = null;
        try {
            File file = new File(dir, fileName);
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(data);
            bos.flush();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String saveFile(Bitmap bitmap, String dir, String fileName) {
        BufferedOutputStream bos = null;
        try {
            File file = new File(dir, fileName);
            bos = new BufferedOutputStream(new FileOutputStream(file));
            if (fileName != null && (fileName.contains(".png") || fileName.contains(".PNG"))) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            }
            bos.flush();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getInternalFileDir(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    /**
     * 获取Internal的cache的存储目录
     * /data/user/0/com.yi.moments/cache
     */
    public static String getInternalCacheDir(Context context) {
        return context.getCacheDir().getAbsolutePath();
    }


    /**
     * 获取Internal的cache下的thumb存储目录
     * /data/user/0/com.yi.moments/cache/videoThumb/
     */
    public static String getInternalThumbCacheDir(Context context) {
        File dir = new File(getInternalCacheDir(context) + "/videoThumb/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    /**
     * 获取SD卡的根目录
     * /storage/emulated/0
     */
    public static String getSDCardBaseDir() {
        if (isSDCardMounted()) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return null;
    }

    /**
     * 获取SD卡公有目录的路径
     * /storage/emulated/0/[type]
     */
    public static String getSDCardPublicDir(String type) {
        if (isSDCardMounted()) {
            return Environment.getExternalStoragePublicDirectory(type).getAbsolutePath();
        }
        return null;
    }

    /**
     * 获取SD卡私有Cache目录的路径
     * /storage/emulated/0/Android/data/com.yi.moments/cache
     */
    public static String getSDCardPrivateCacheDir(Context context) {
        if (isSDCardMounted() && context.getExternalCacheDir() != null) {
            return context.getExternalCacheDir().getAbsolutePath();
        }
        return null;
    }

    /**
     * 获取SD卡私有Files目录的路径
     * /storage/emulated/0/Android/data/com.yi.moments/files/[dir]
     */
    public static String getSDCardPrivateFilesDir(Context context, String dir) {
        if (isSDCardMounted() && context.getExternalFilesDir(dir) != null) {
            return context.getExternalFilesDir(dir).getAbsolutePath();
        }
        return null;
    }


    // 判断SD卡是否被挂载
    public static boolean isSDCardMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static String getFormatPath(Context context, String folder, String suffix) {
        String dir = getSDCardPrivateCacheDir(context);
        if (dir == null) {
            dir = getInternalCacheDir(context);
        }
        String path = dir + File.separator + folder;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        path += File.separator + System.currentTimeMillis() + suffix;
        return path;
    }

    public static String getFormatFolder(Context context, String folder) {
        String dir = getSDCardPrivateCacheDir(context);
        if (dir == null) {
            dir = getInternalCacheDir(context);
        }
        String folderPath = dir + File.separator + folder;
        File folderFile = new File(folderPath);
        if (!folderFile.exists()) {
            folderFile.mkdir();
        }
        return folderPath;
    }

    public static String getFormatVideoPath(Context context) {
        return getFormatPath(context, VIDEO_FOLDER, ".mp4");
    }

    public static String getFormatVideoTempPath(Context context) {
        return getFormatPath(context, TEMP_FOLDER, ".mp4");
    }

    public static String getFormatRawPath(Context context) {
        return getFormatPath(context, RAW_FOLDER, ".yuv");
    }

    public static String getCameraVideoFolder(Context context) {
        return getFormatFolder(context, CAMERA_VIDEO_FOLDER);
    }

    public static String getCameraVideoPath(Context context) {
        return getFormatPath(context, CAMERA_VIDEO_FOLDER, ".mp4");
    }

    public static String getRawFolder(Context context) {
        return getFormatFolder(context, RAW_FOLDER);
    }

    public static String getRawCacheFolder(Context context) {
        return getFormatFolder(context, RAW_CACHE_FOLDER);
    }

    public static String getRawCachePath(Context context) {
        return getFormatPath(context, RAW_CACHE_FOLDER, ".dat");
    }

    public static String getVideoThumbnailTempPath(Context context) {
        return getFormatPath(context, VIDEO_FOLDER, ".jpg");
    }

    public static String getCoverPageThumbnailPath(Context context) {
        return getFormatPath(context, COVER_PAGE_FOLDER, ".png");
    }

    public static void clearCoverPageThumbnailFolder(Context context) {
        String fileFolder = getFormatFolder(context, COVER_PAGE_FOLDER);
        clearFolder(fileFolder);
    }

    public static void clearRawCacheFolder(Context context) {
        String fileFolder = getRawCacheFolder(context);
        clearFolder(fileFolder);
    }

    public static void clearFolder(String fileFolder) {
        LogUtils.d("StorageUtil", "clearFolder:" + fileFolder);
        if (!TextUtils.isEmpty(fileFolder)) {
            File folder = new File(fileFolder);
            if (folder != null) {
                if (folder.isDirectory()) {
                    File[] files = folder.listFiles();
                    for (File file : files) {
                        if (file.isDirectory()) {
                            clearFolder(file.toString());
                        } else {
                            file.delete();
                        }
                    }
                } else {
                    folder.delete();
                }
            }
        }
    }

    public static boolean saveBitmap(String picPath, Bitmap bitmap) {
        if (bitmap == null) {
            return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(picPath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 保存Bitmap到sdcard
     *
     * @param b
     */
    public static void saveBitmap(Bitmap b) {
        String path = initPath();
        long dataTake = System.currentTimeMillis();
        String jpegName = path + "/" + "moreinfo" + ".jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            bos.flush();
            bos.close();
            Log.e("===", "saveBitmap成功");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e("===", "saveBitmap:失败");
            e.printStackTrace();
        }
    }

    /**
     * 初始化保存路径
     *
     * @return
     */
    public static String initPath() {
        if (storagePath.equals("")) {
            storagePath = parentPath.getAbsolutePath();
            File f = new File(storagePath);
            if (!f.exists()) {
                f.mkdir();
            }
        }
        return storagePath;
    }

    public static void deleteFile(String... filePaths) {
        if (filePaths == null) {
            return;
        }
        for (String path : filePaths) {
            if (TextUtils.isEmpty(path)) {
                continue;
            }
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public static void createFile(String... filePaths) {
        if (filePaths == null) {
            return;
        }
        for (String path : filePaths) {
            if (TextUtils.isEmpty(path)) {
                continue;
            }
            File file = new File(path);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getLrcContent(String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String result = "";
            while ((line = bufReader.readLine()) != null) {
                if (line.trim().equals("")) {
                    continue;
                }
                result += line + "\r\n";
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static long getFileSize(File file) {
        long size = 0;
        if (file != null && file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                size = fis.available();
                fis.close();
            } catch (Exception e) {

            }
        }
        return size;
    }

    // 检测文件存在
    private static boolean checkFile(String filePath) {
        //boolean result = FileUtil.fileIsExist(filePath);
        boolean result = false;
        File mFile = new File(filePath);
        if (mFile.exists()) {
            result = true;
        }
        return result;
    }

    // 获得转化后的时间
    private static long getTimeWrap(long time) {
        if (time <= 0) {
            return System.currentTimeMillis();
        }
        return time;
    }

    /**
     * 针对非系统文件夹下的文件,使用该方法
     * 插入时初始化公共字段
     *
     * @param filePath 文件
     * @param time     ms
     * @return ContentValues
     */
    private static ContentValues initCommonContentValues(String filePath, long time) {
        ContentValues values = new ContentValues();
        File saveFile = new File(filePath);
        long timeMillis = getTimeWrap(time);
        values.put(MediaStore.MediaColumns.TITLE, saveFile.getName());
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, saveFile.getName());
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, timeMillis);
        values.put(MediaStore.MediaColumns.DATE_ADDED, timeMillis);
        values.put(MediaStore.MediaColumns.DATA, saveFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.SIZE, saveFile.length());
        return values;
    }


    /**
     * 保存到视频到本地，并插入MediaStore以保证相册可以查看到,这是更优化的方法，防止读取的视频获取不到宽高
     *
     * @param context    上下文
     * @param filePath   文件路径
     * @param createTime 创建时间 <=0时为当前时间 ms
     * @param duration   视频长度 ms
     * @param width      宽度
     * @param height     高度
     */
    public static void insertVideoToMediaStore(Context context, String filePath, long createTime, int width, int height, long duration, ICallback callback) {
        if (!checkFile(filePath)) {
            callback.onError(null);
            return;
        }
        createTime = getTimeWrap(createTime);
        ContentValues values = initCommonContentValues(filePath, createTime);
        values.put(MediaStore.Video.VideoColumns.DATE_TAKEN, createTime);
        if (duration > 0) { values.put(MediaStore.Video.VideoColumns.DURATION, duration); }
        if (width > 0) { values.put(MediaStore.Video.VideoColumns.WIDTH, width); }
        if (height > 0) { values.put(MediaStore.Video.VideoColumns.HEIGHT, height); }
        values.put(MediaStore.MediaColumns.MIME_TYPE, getVideoMimeType(filePath));
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        callback.onResult(null);
    }

    // 获取video的mine_type,暂时只支持mp4,3gp
    private static String getVideoMimeType(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith("mp4") || lowerPath.endsWith("mpeg4")) {
            return "video/mp4";
        } else if (lowerPath.endsWith("3gp")) {
            return "video/3gp";
        }
        return "video/mp4";
    }
}
