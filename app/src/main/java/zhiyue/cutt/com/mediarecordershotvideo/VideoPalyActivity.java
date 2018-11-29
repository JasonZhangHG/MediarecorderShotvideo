package zhiyue.cutt.com.mediarecordershotvideo;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoPalyActivity extends AppCompatActivity {

    @BindView(R.id.vv_show_video_video_play_activity) VideoView mVideoView;
    private String mVideoPath = "/storage/emulated/0/DCIM/monkey/1543487225567.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_paly);
        ButterKnife.bind(this);
        startPlayVideo(mVideoPath);
    }

    public void startPlayVideo(final String videoPath) {
        mVideoView.setVideoPath(videoPath);
        mVideoView.start();
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
            }
        });
    }

}
