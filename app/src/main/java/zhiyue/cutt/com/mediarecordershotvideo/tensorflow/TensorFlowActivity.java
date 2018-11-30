package zhiyue.cutt.com.mediarecordershotvideo.tensorflow;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.LogUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import zhiyue.cutt.com.mediarecordershotvideo.R;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TensorFlowActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener {

    @BindView(R.id.iv_show_image_tensorflow_activity) ImageView mShowImage;
    @BindView(R.id.tv_tensorflow_activity) TextView mResultText;
    @BindView(R.id.btn_start_capture_tensor_flow_activity) Button mShutterClick;
    private String mImagePath;
    private TtsSpeaker mTtsSpeaker;
    private ImagePreprocessor mImagePreprocessor;
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;
    private static final int TF_INPUT_IMAGE_WIDTH = 224;
    private static final int TF_INPUT_IMAGE_HEIGHT = 224;
    private TextToSpeech mTtsEngine;
    private AtomicBoolean mReady = new AtomicBoolean(false);
    private TensorFlowImageClassifier mTensorFlowClassifier;
    private CameraHandler mCameraHandler;
    private Bitmap mCurrentBitmap;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tensor_flow);
        ButterKnife.bind(this);
        mImagePath = getIntent().getStringExtra("ImagePath");

//        Glide.with(TensorFlowActivity.this)
//                .load(mImagePath)
//                .fitCenter()
//                .dontAnimate()
//                .into(mShowImage);

        Glide.with(TensorFlowActivity.this).load(mImagePath).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                mCurrentBitmap = resource;
                mShowImage.setImageBitmap(mCurrentBitmap);
                mBackgroundThread = new HandlerThread("BackgroundThread");
                mBackgroundThread.start();
                mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
                mBackgroundHandler.post(mInitializeOnBackground);
            }
        });

    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            mImagePreprocessor = new ImagePreprocessor(PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT,
                    TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT);

            mTtsSpeaker = new TtsSpeaker();
            mTtsSpeaker.setHasSenseOfHumor(true);
            mTtsEngine = new TextToSpeech(TensorFlowActivity.this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        mTtsEngine.setLanguage(Locale.US);
                        mTtsEngine.setOnUtteranceProgressListener(utteranceListener);
                        mTtsSpeaker.speakReady(mTtsEngine);
                    } else {
                        LogUtils.d("TensorFlowActivity Could not open TTS Engine (onInit status=" + status + "). Ignoring text to speech");
                        mTtsEngine = null;
                    }
                }
            });
//            mCameraHandler = CameraHandler.getInstance();
//            mCameraHandler.initializeCamera(TensorFlowActivity.this, PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, mBackgroundHandler, (ImageReader.OnImageAvailableListener) TensorFlowActivity.this);
            try {
                mTensorFlowClassifier = new TensorFlowImageClassifier(TensorFlowActivity.this, mShowImage.getWidth(), mShowImage.getHeight());
            } catch (IOException e) {
                throw new IllegalStateException("Cannot initialize TFLite Classifier", e);
            }
            setReady(true);
        }
    };

    private UtteranceProgressListener utteranceListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            setReady(false);
        }

        @Override
        public void onDone(String utteranceId) {
            setReady(true);
        }

        @Override
        public void onError(String utteranceId) {
            setReady(true);
        }
    };

    /**
     * Mark the system as ready for a new image capture
     */
    private void setReady(boolean ready) {
        mReady.set(ready);
    }

    @OnClick(R.id.btn_start_capture_tensor_flow_activity)
    public void startImageCapture() {
        LogUtils.d("TensorFlowActivity startImageCapture() Ready for another capture? " + mReady.get());
        if (mReady.get()) {
            setReady(false);
            mResultText.setText("Hold on...");
            mResultText.post(mBackgroundClickHandler);
        } else {
            LogUtils.d("Sorry, processing hasn't finished. Try again in a few seconds");
        }
    }

    private Runnable mBackgroundClickHandler = new Runnable() {
        @Override
        public void run() {
            if (mTtsEngine != null) {
                mTtsSpeaker.speakShutterSound(mTtsEngine);
            }
//            mCameraHandler.takePicture();

            checkImage(mCurrentBitmap);
        }
    };


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        final Bitmap bitmap;
        try (Image image = imageReader.acquireNextImage()) {
            bitmap = mImagePreprocessor.preprocessImage(image);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShowImage.setImageBitmap(bitmap);
            }
        });
        checkImage(bitmap);
    }

    public void checkImage(Bitmap bitmap) {
        final Collection<Recognition> results = mTensorFlowClassifier.doRecognize(bitmap);
        LogUtils.d("onImageAvailable() Got the following results from Tensorflow: " + results);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (results == null || results.isEmpty()) {
                    mResultText.setText("I don't understand what I see");
                } else {
                    StringBuilder sb = new StringBuilder();
                    Iterator<Recognition> it = results.iterator();
                    int counter = 0;
                    while (it.hasNext()) {
                        Recognition r = it.next();
                        sb.append(r.getTitle());
                        counter++;
                        if (counter < results.size() - 1) {
                            sb.append(", ");
                        } else if (counter == results.size() - 1) {
                            sb.append(" or ");
                        }
                    }
                    mResultText.setText(sb.toString());
                }
            }
        });

        if (mTtsEngine != null) {
            // speak out loud the result of the image recognition
            mTtsSpeaker.speakResults(mTtsEngine, results);
        } else {
            // if theres no TTS, we don't need to wait until the utterance is spoken, so we set
            // to ready right away.
            setReady(true);
        }
    }
}
