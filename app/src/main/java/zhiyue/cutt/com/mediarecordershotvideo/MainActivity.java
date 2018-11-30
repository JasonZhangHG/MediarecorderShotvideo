package zhiyue.cutt.com.mediarecordershotvideo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.bumptech.glide.Glide;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import zhiyue.cutt.com.mediarecordershotvideo.tensorflow.TensorFlowActivity;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.su_main_activity) SurfaceView mSurfaceView;
    @BindView(R.id.btn_take_photo_main_activity) Button mTakePhoto;
    @BindView(R.id.btn_shot_video_main_activity) Button mShotVideo;
    @BindView(R.id.btn_switch_camera_main_activity) Button mSwitchCamera;
    @BindView(R.id.iv_show_photo_main_activity) ImageView mShowPhoto;
    @BindView(R.id.vv_show_video_main_activity) VideoView mShowVideo;
    private int clickShotVideoCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getGetWritePermission();
        mSurfaceView.setZOrderMediaOverlay(false);
        mSurfaceView.setZOrderOnTop(false);
        mShowVideo.setZOrderOnTop(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            LogUtils.d("onResume()  OpenCVLoader.initAsync()");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            LogUtils.d("onResume()  mLoaderCallback.onManagerConnected()");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        CameraManager.getInstance().startPreview(mSurfaceView, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraManager.getInstance().stopPreview(true);
    }

    public void startPlayVideo(final String videoPath) {
        mShowVideo.setVideoPath(videoPath);
        mShowVideo.start();
        mShowVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
            }
        });
    }

    public void getGetWritePermission() {
        PermissionUtils.permission(PermissionConstants.STORAGE, PermissionConstants.CAMERA, PermissionConstants.MICROPHONE)
                .rationale(new PermissionUtils.OnRationaleListener() {
                    @Override
                    public void rationale(final ShouldRequest shouldRequest) {
                        shouldRequest.again(true);
                    }
                })
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(List<String> permissionsGranted) {
                        CameraManager.getInstance().startPreview(mSurfaceView, true);
                    }

                    @Override
                    public void onDenied(List<String> permissionsDeniedForever,
                                         List<String> permissionsDenied) {
                        if (!permissionsDeniedForever.isEmpty()) {
                            PermissionUtils.launchAppDetailsSettings();
                        }
                    }
                }).request();
    }

    @OnClick(R.id.btn_shot_video_main_activity)
    public void startShotVideo() {
        if (clickShotVideoCount % 2 == 0) {
            if (mShowVideo.isPlaying()) {
                mShowVideo.pause();
            }
            LogUtils.d("MainActivity startShotVideo()");
            CameraManager.getInstance().startShotVideo();
            mShotVideo.setText("停止录像");
        } else {
            LogUtils.d("MainActivity endShotVideo()");
            CameraManager.getInstance().endShotVideo(new ICallback<String>() {
                @Override
                public void onResult(final String videoPath) {
                    mShowVideo.post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.d("MainActivity endShotVideo()  videoPath = " + videoPath);
                            startPlayVideo(videoPath);
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {

                }
            });
            mShotVideo.setText("开始录像");
        }
        clickShotVideoCount++;
    }

    @OnClick(R.id.btn_take_photo_main_activity)
    public void takePhoto() {
        LogUtils.d("MainActivity takePhoto()");
        CameraManager.getInstance().takePhoto(new ICallback<String>() {
            @Override
            public void onResult(final String picPath) {
                LogUtils.d("MainActivity takePhoto()  onResult = " + picPath);
                mShowPhoto.post(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(MainActivity.this)
                                .load(picPath)
                                .fitCenter()
                                .dontAnimate()
                                .into(mShowPhoto);

                        Intent intent = new Intent(MainActivity.this, TensorFlowActivity.class);
                        intent.putExtra("ImagePath", picPath);
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onError(Throwable error) {

            }
        });
    }

    @OnClick(R.id.btn_switch_camera_main_activity)
    public void switchCamera() {
        CameraManager.getInstance().switchCamera();
    }

    /***
     * opencv库 加载并初始化回调的函数
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    LogUtils.d("OpenCv load success");
                    break;
                default:
                    super.onManagerConnected(status);
                    LogUtils.d("OpenCv load failed");
                    break;
            }
        }
    };
}
