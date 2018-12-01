package zhiyue.cutt.com.mediarecordershotvideo.opengl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class OpenGLActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_open_gl);
        glSurfaceView = new GLSurfaceView(this);
        if (supportEs2()) {
            glSurfaceView.setEGLContextClientVersion(2);// 指定EGL版本
            glSurfaceView.setRenderer(new HockeyRenderer());
            rendererSet = true;
        } else {
            Toast.makeText(this, "该设备不支持OpenGL.ES 2.0", Toast.LENGTH_SHORT).show();
        }
        setContentView(glSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }

    public boolean supportEs2() {
        //检查系统是否支持OpenGL.ES 2.0
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo deviceConfigurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportEs2 = deviceConfigurationInfo.reqGlEsVersion >= 0x20000;
        return supportEs2;
    }

}
