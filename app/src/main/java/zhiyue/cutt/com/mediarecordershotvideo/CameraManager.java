package zhiyue.cutt.com.mediarecordershotvideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.blankj.utilcode.util.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.graphics.Bitmap.createBitmap;

public class CameraManager {

    private static volatile CameraManager mCameraManager;
    private static final int BEST_PREVIEW_WIDTH = 1280;
    private static final int BEST_PREVIEW_HEIGHT = 720;
    private CameraHandler mCameraHandler;
    private static final Object PREVIEW_LOCK = new Object();

    public static CameraManager getInstance() {
        if (mCameraManager == null) {
            synchronized (CameraManager.class) {
                if (mCameraManager == null) {
                    mCameraManager = new CameraManager();
                }
            }
        }
        return mCameraManager;
    }

    public void startPreview(SurfaceView surfaceView, boolean isOpenFrontCamera) {
        LogUtils.d("CameraManager startPreview()", surfaceView, isOpenFrontCamera);
        synchronized (PREVIEW_LOCK) {
            if (mCameraHandler == null) {
                final CameraThread thread = new CameraThread();
                thread.setPriority(Thread.MAX_PRIORITY);
                thread.start();
                mCameraHandler = new CameraHandler(thread.getLooper(), thread);
            }
            mCameraHandler.startPreview(BEST_PREVIEW_WIDTH, BEST_PREVIEW_HEIGHT, surfaceView, isOpenFrontCamera);
        }
    }

    public void stopPreview(boolean needWait) {
        LogUtils.d("stopPreview needWait = " + needWait);
        synchronized (PREVIEW_LOCK) {
            if (mCameraHandler != null) {
                mCameraHandler.stopPreview(needWait);
            }
            mCameraHandler = null;
        }
    }

    public void switchCamera() {
        LogUtils.d("switchCamera（）");
        synchronized (PREVIEW_LOCK) {
            if (mCameraHandler != null) {
                mCameraHandler.switchCamera();
            }
        }
    }

    public void takePhoto(ICallback<String> callback) {
        LogUtils.d("takePhoto（）");
        synchronized (PREVIEW_LOCK) {
            if (mCameraHandler != null) {
                mCameraHandler.takePhoto(callback);
            }
        }
    }

    public void startShotVideo() {
        LogUtils.d("startShotVideo（）");
        synchronized (PREVIEW_LOCK) {
            if (mCameraHandler != null) {
                mCameraHandler.startShotVideo();
            }
        }
    }

    public void endShotVideo(ICallback<String> callback) {
        LogUtils.d("endShotVideo（）");
        synchronized (PREVIEW_LOCK) {
            if (mCameraHandler != null) {
                mCameraHandler.endShotVideo(callback);
            }
        }
    }

    /**
     * Handler class for asynchronous camera operation
     */
    private static final class CameraHandler extends Handler {

        private static final int MSG_PREVIEW_START = 1;
        private static final int MSG_PREVIEW_STOP = 2;
        private static final int MSG_PREVIEW_BUFFER_ADD = 3;
        private static final int MSG_PREVIEW_SET_TEXTURE = 4;
        private static final int MSG_PREVIEW_SWITCH_CAMERA = 5;
        private static final int MSG_PREVIEW_TALE_PHOTO = 6;
        private static final int MSG_PREVIEW_SHOT_VIDEO = 7;
        private static final int MSG_PREVIEW_START_SHOT_VIDEO = 8;
        private static final int MSG_PREVIEW_END_SHOT_VIDEO = 9;

        private CameraThread mThread;

        private CameraHandler(Looper looper, final CameraThread thread) {
            super(looper);
            mThread = thread;
        }

        private void startPreview(final int width, final int height, final SurfaceView surfaceView, boolean isOpenFrontCamera) {
            LogUtils.d("CameraHandler startPreview（）");
            sendMessage(obtainMessage(MSG_PREVIEW_START, new Object[]{width, height, surfaceView, isOpenFrontCamera}));
        }

        private void addPreviewBuffer(byte[] bytes) {
            sendMessage(obtainMessage(MSG_PREVIEW_BUFFER_ADD, bytes));
        }

        private void setPreviewTexture(SurfaceTexture surfaceTexture) {
            sendMessage(obtainMessage(MSG_PREVIEW_SET_TEXTURE, surfaceTexture));
        }

        private void takePhoto(ICallback<String> callback) {
            LogUtils.d("CameraHandler takePhoto（）");
            sendMessage(obtainMessage(MSG_PREVIEW_TALE_PHOTO, callback));
        }

        /**
         * request to stop camera preview
         *
         * @param needWait need to wait for stopping camera preview
         */
        private void stopPreview(boolean needWait) {
            LogUtils.d("CameraHandler stopPreview（）");
            synchronized (this) {
                sendEmptyMessage(MSG_PREVIEW_STOP);
                if (needWait && mThread.isAlive()) {
                    try {
                        LogUtils.d("wait for terminating of camera thread");
                        wait();
                    } catch (final InterruptedException e) {
                    }
                }
            }
        }

        public void switchCamera() {
            LogUtils.d("CameraHandler switchCamera（）");
            synchronized (this) {
                sendEmptyMessage(MSG_PREVIEW_SWITCH_CAMERA);
            }
        }

        public void startShotVideo() {
            LogUtils.d("CameraHandler startShotVideo（）");
            synchronized (this) {
                sendEmptyMessage(MSG_PREVIEW_START_SHOT_VIDEO);
            }
        }

        public void endShotVideo(ICallback<String> callback) {
            LogUtils.d("CameraHandler endShotVideo（）");
            synchronized (this) {
                sendMessage(obtainMessage(MSG_PREVIEW_END_SHOT_VIDEO, callback));
            }
        }

        /**
         * message handler for camera thread
         */
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_PREVIEW_START: {
                    Object[] objs = (Object[]) msg.obj;
                    mThread.startPreview((int) objs[0], (int) objs[1], (SurfaceView) objs[2], (Boolean) objs[3], true);
                    break;
                }
                case MSG_PREVIEW_STOP: {
                    mThread.stopPreview();
                    synchronized (this) {
                        notifyAll();
                    }
                    Looper.myLooper().quit();
                    mThread = null;
                    break;
                }

                case MSG_PREVIEW_TALE_PHOTO: {
                    mThread.takePhoto((ICallback<String>) msg.obj);
                    break;
                }

                case MSG_PREVIEW_START_SHOT_VIDEO: {
                    mThread.startShotVideo();
                    break;
                }

                case MSG_PREVIEW_END_SHOT_VIDEO: {
                    mThread.endShotVideo((ICallback<String>) msg.obj);
                    break;
                }

                case MSG_PREVIEW_SWITCH_CAMERA: {
                    mThread.switchCamera();
                    break;
                }
                default:
                    throw new RuntimeException("unknown message:what=" + msg.what);
            }
        }
    }


    /**
     * Thread for asynchronous operation of camera preview
     */
    private static final class CameraThread extends HandlerThread implements Camera.PreviewCallback {


        private Camera.CameraInfo mFrontCameraInfo = null;
        private int mFrontCameraId = -1;
        private Camera.CameraInfo mBackCameraInfo = null;
        private int mBackCameraId = -1;
        private int mCameraId;
        private Camera mCamera;
        private int mViewWidth;
        private int mViewHeight;
        private byte[] nv21Data;
        private Camera.Size mPreviewSize = null;
        private Camera.Size mPictureSize = null;
        private SurfaceView msurfaceView;
        private MediaRecorder mMediaRecorder;
        private String videoPath;
        private Camera.Parameters mParams;
        private int cameraAngle = 90;//摄像头角度   默认为90度
        private int angle = 0;
        private int rotation = 0;
        private SensorManager sm = null;

        public CameraThread() {
            super("Camera thread");
            initCameraInfo();
        }

        /**
         * 1.初始化摄像头信息。
         */
        private void initCameraInfo() {
            LogUtils.d("CameraThread initCameraInfo（）");
            int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
            for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(cameraId, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    // 后置摄像头信息
                    mBackCameraId = cameraId;
                    mBackCameraInfo = cameraInfo;
                } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    // 前置摄像头信息
                    mFrontCameraId = cameraId;
                    mFrontCameraInfo = cameraInfo;
                }
            }
        }

        private void startPreview(final int width, final int height, final SurfaceView surfaceView, boolean isOpenFrontCamera, boolean isFirstOpen) {
            LogUtils.d("CameraThread startPreview（）width = " + width + "  height = " + height + "  isOpenFrontCamera = " + isOpenFrontCamera + "  isFirstOpen = " + isFirstOpen);
            this.msurfaceView = surfaceView;
            if (isFirstOpen) {
                if (isOpenFrontCamera) {
                    mCameraId = mFrontCameraId;
                } else {
                    mCameraId = mBackCameraId;
                }
            }
            mViewWidth = width;
            mViewHeight = height;
            if (mCamera == null) {
                try {
                    openCamera(width, height);
                    final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                    mViewWidth = previewSize.width;
                    mViewHeight = mPictureSize.height;
                    byte[] preAllocedBuffer = new byte[mViewWidth * mViewHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21)];
                    mCamera.addCallbackBuffer(preAllocedBuffer);
//                    mCamera.setPreviewCallback(this);
                } catch (final RuntimeException e) {
                    if (mCamera != null) {
                        mCamera.release();
                        mCamera = null;
                    }
                }
                if (mCamera != null) {
                    mCamera.startPreview();
                }
            }
            if (mCamera != null && msurfaceView != null) {
                try {
                    mCamera.setPreviewDisplay(msurfaceView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            registerSensorManager();
        }


        private void openCamera(int width, int height) {
            LogUtils.d("CameraThread openCamera（）width = " + width + "  height = " + height);
            if (mCamera != null) {
                throw new RuntimeException("camera already initialized");
            }
            mCamera = Camera.open(mCameraId);
            if (mCamera == null) {
                throw new RuntimeException("unable to open camera");
            }

            final Camera.Parameters params = mCamera.getParameters();
            // let's try fastest frame rate. You will get near 60fps, but your device become hot.
            final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();
            final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
            params.setPreviewFpsRange(max_fps[0], max_fps[1]);

            // 优化后的camera FPS设置方法
            int framerate = 30 * 1000;
            List<int[]> rates = params.getSupportedPreviewFpsRange();
            int[] closetFramerate = rates.get(0);
            for (int i = 0; i < rates.size(); i++) {
                int[] rate = rates.get(i);
                int curDelta = Math.abs(rate[1] - framerate);
                int bestDelta = Math.abs(closetFramerate[1] - framerate);
                if (curDelta < bestDelta) {
                    closetFramerate = rate;
                } else if (curDelta == bestDelta) {
                    closetFramerate = closetFramerate[0] < rate[0] ? rate : closetFramerate;
                }
            }
            params.setPreviewFpsRange(closetFramerate[0], closetFramerate[1]);
            params.setRecordingHint(true);
            // request closest supported preview size
            mPreviewSize = getClosestSupportedSize(params.getSupportedPreviewSizes(), width, height);
            params.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

            // request closest picture size for an aspect ratio issue on Nexus7
            mPictureSize = getClosestSupportedSize(params.getSupportedPictureSizes(), width, height);
            params.setPictureSize(mPictureSize.width, mPictureSize.height);
            // rotate camera preview according to the device orientation
            setRotation(params);
            mCamera.setParameters(params);
        }

        /**
         * rotate preview screen according to the device orientation
         *
         * @param params
         */
        private void setRotation(final Camera.Parameters params) {
            final Display display = ((WindowManager) CCApplication.getInstance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            final int rotation = display.getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
                default:
                    break;
            }
            // get whether the camera is front camera or back camera
            final Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, info);
            int orientation = info.orientation;
            boolean issFrontFace = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
            if (issFrontFace) {    // front camera
                degrees = (orientation + degrees) % 360;
                degrees = (360 - degrees) % 360;  // reverse
            } else {  // back camera
                degrees = (orientation - degrees + 360) % 360;
            }
            // apply rotation setting
            mCamera.setDisplayOrientation(degrees);
            cameraAngle = degrees;
        }

        private static Camera.Size getClosestSupportedSize(List<Camera.Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
            return Collections.min(supportedSizes, new Comparator<Camera.Size>() {

                private int diff(final Camera.Size size) {
                    return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
                }

                @Override
                public int compare(final Camera.Size lhs, final Camera.Size rhs) {
                    return diff(lhs) - diff(rhs);
                }
            });
        }

        public void stopPreview() {
            LogUtils.d("CameraThread stopPreview()");
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                mPreviewSize = null;
                mPictureSize = null;
                nv21Data = null;
                unregisterSensorManager();
            }
        }

        public void switchCamera() {
            LogUtils.d("CameraThread switchCamera()");
            stopPreview();
            if (mCameraId == mFrontCameraId) {
                mCameraId = mBackCameraId;
            } else {
                mCameraId = mFrontCameraId;
            }
            startPreview(mViewWidth, mViewHeight, msurfaceView, false, false);
        }

        void registerSensorManager() {
            if (sm == null) {
                sm = (SensorManager) CCApplication.getInstance().getSystemService(Context.SENSOR_SERVICE);
            }
            sm.registerListener(sensorEventListener, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }

        void unregisterSensorManager() {
            if (sm == null) {
                sm = (SensorManager) CCApplication.getInstance().getSystemService(Context.SENSOR_SERVICE);
            }
            sm.unregisterListener(sensorEventListener);
        }

        private SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (Sensor.TYPE_ACCELEROMETER != event.sensor.getType()) {
                    return;
                }
                float[] values = event.values;
                angle = AngleUtil.getSensorAngle(values[0], values[1]);
                rotationAnimation();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };


        //切换摄像头icon跟随手机角度进行旋转
        private void rotationAnimation() {
            if (rotation != angle) {
                int start_rotaion = 0;
                int end_rotation = 0;
                switch (rotation) {
                    case 0:
                        start_rotaion = 0;
                        switch (angle) {
                            case 90:
                                end_rotation = -90;
                                break;
                            case 270:
                                end_rotation = 90;
                                break;
                        }
                        break;
                    case 90:
                        start_rotaion = -90;
                        switch (angle) {
                            case 0:
                                end_rotation = 0;
                                break;
                            case 180:
                                end_rotation = -180;
                                break;
                        }
                        break;
                    case 180:
                        start_rotaion = 180;
                        switch (angle) {
                            case 90:
                                end_rotation = 270;
                                break;
                            case 270:
                                end_rotation = 90;
                                break;
                        }
                        break;
                    case 270:
                        start_rotaion = 90;
                        switch (angle) {
                            case 0:
                                end_rotation = 0;
                                break;
                            case 180:
                                end_rotation = 180;
                                break;
                        }
                        break;
                }
                rotation = angle;
            }
        }

        /**
         * 拍照
         */
        private int nowAngle;

        public void takePhoto(final ICallback<String> callback) {
            if (mCamera == null) {
                return;
            }
            switch (cameraAngle) {
                case 90:
                    nowAngle = Math.abs(angle + cameraAngle) % 360;
                    break;
                case 270:
                    nowAngle = Math.abs(cameraAngle - angle);
                    break;
                default:
                    break;
            }
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes, Camera camera) {
                    camera.startPreview();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    Matrix matrix = new Matrix();
                    if (mCameraId == mBackCameraId) {
                        matrix.setRotate(nowAngle);
                    } else if (mCameraId == mFrontCameraId) {
                        matrix.setRotate(360 - nowAngle);
                        matrix.postScale(-1, 1);
                    }
                    bitmap = createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    String bitMapFileName = Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM/monkey" + File.separator + System.currentTimeMillis() + ".jpg";
                    StorageUtil.saveBitmap(bitMapFileName, bitmap);
                    if (callback != null) {
                        callback.onResult(bitMapFileName);
                    }
                }
            });
        }

        public void initMediaRecorder() {
            if (mCamera == null) {return;}
            LogUtils.d("CameraThread initMediaRecorder()");
            try {
                mMediaRecorder = new MediaRecorder();
                if (mParams == null) {
                    mParams = mCamera.getParameters();
                }
                List<String> focusModes = mParams.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }
                // 这两项需要放在setOutputFormat之前
                mCamera.setParameters(mParams);
                mCamera.unlock();
                mMediaRecorder.reset();
                mMediaRecorder.setCamera(mCamera);
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//设置视频编码方式
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                // Set output file format//设置文件输出格式
                mMediaRecorder.setVideoSize(mViewWidth, mViewHeight);//设置视频宽和高
                mMediaRecorder.setVideoFrameRate(30);//设置视频帧率
                mMediaRecorder.setVideoEncodingBitRate(1024 * 1024);//设置所录制视频的编码位率。
                mMediaRecorder.setOrientationHint(270);//设置输出的视频播放的方向提示。
                mMediaRecorder.setMaxDuration(10 * 1000);
                mMediaRecorder.setPreviewDisplay(msurfaceView.getHolder().getSurface());//设置使用哪个SurfaceView来显示视频预览。
            } catch (Exception e) {
                LogUtils.d("CameraThread startShotVideo() catch e = " + e);
            }
        }

        public void startShotVideo() {
            LogUtils.d("CameraThread startShotVideo()");
            try {
                mCamera.setPreviewCallback(null);
                initMediaRecorder();
                String path = Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM/monkey" + File.separator;
                File file = new File(path);
                if (!file.exists()) {
                    file.mkdirs();
                }
                videoPath = path + System.currentTimeMillis() + ".mp4";
                mMediaRecorder.setOutputFile(videoPath);
                mMediaRecorder.prepare();
                mMediaRecorder.start();
            } catch (Exception e) {
                LogUtils.d("CameraThread startShotVideo() catch e = " + e);
            }
        }

        public void endShotVideo(ICallback<String> callback) {
            LogUtils.d("CameraThread endShotVideo() callback = " + callback);
            if (mMediaRecorder != null) {
                try {
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.setPreviewDisplay(null);
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                    mMediaRecorder = null;
                } catch (Exception e) {
                    e.printStackTrace();
                    mMediaRecorder = null;
                }
            }
            if (callback != null) {
                callback.onResult(videoPath);
            }
        }

        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            if (bytes == null) { return; }
            if (mPreviewSize == null) { return; }
            if (nv21Data == null) {
                nv21Data = new byte[bytes.length];
            }
            //mPreviewSize = mCamera.getParameters().getPreviewSize();
            System.arraycopy(bytes, 0, nv21Data, 0, bytes.length);
            //nv21Data = data;
            camera.addCallbackBuffer(nv21Data);
        }
    }
}
