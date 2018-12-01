package zhiyue.cutt.com.mediarecordershotvideo.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.blankj.utilcode.util.LogUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 在幕后，GLSurfaceView实际上为它自己创建了一个窗口（window），
 * 并在视图层次（View Hierarchy）上穿了个“洞”，让底层的OpenGL surface显示出来。
 * 对于大多数使用情况，这足够了；但是，GLSurfaceView与常规视图（view）不同，
 * 它没有动画或者变形特效，因为GLSurfaceView是窗口（window）的一部分。
 * 从Android 4.0开始，Android提供了一个纹理视图（TextureView），
 * 它可以渲染OpenGL而不用创建单独的窗口或打洞了，这意味着，
 * 这个视图像一个常规视图（view）一样，可以被操作，且有动画和变形特效。
 * 但是，TextureView类没有内置OpenGL初始化操作，要想使用TextureView，
 * 一种方法是执行自定义的OpenGL初始化并在TextureView上运行，另外一种方法是把GLSurfaceView的源代码拿出来，
 * 把它适配到TextureView上。这里 还真有网友做出来，但是并不完善和科学，以后我将教大家怎样科学地写一个完善的GL环境。
 */

public class HockeyRenderer implements GLSurfaceView.Renderer {

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        LogUtils.d("HockeyRenderer onSurfaceCreated() gl10 = " + gl10 + "  eglConfig = " + eglConfig);
        /**
         *    当Surface被创建的时候，GLSurfaceView会调用这个方法；
         *    这发生在应用程序第一次运行的时候，并且当设备长时间休眠，
         *    系统回收资源后重新被唤醒，这个方法也可能会被调用。这意味着，本方法可能会被调用多次。
         *
         *    我们调用GLES20.glClearColor(float red, float green, float blue, float alpha);
         *   设置清空屏幕用的颜色；前三个参数分别对应红，绿和蓝，最后的参数对应透明度。
         */
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 0.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        /**
         *  在Surface被创建以后，每次Surface尺寸变化时，在横竖屏切换的时候，这个方法都会被调用到。
         *   我们调用 GLES20.glViewport(int x, int y, int width, int height);
         *  设置视口的尺寸，这个视口尺寸怎么理解呢，就是锁定你操作的渲染区域是哪部分，
         *  整个屏幕那就是 (0.0)点开始，宽度为widht，长度为hight咯。
         *  如果你只想渲染左半个屏幕，那就(0.0)，宽度width/2，长度为hight/2。这样设置viewport大小后，
         *  你之后的GL画图操作，都只作用这部分区域，右半屏幕是不会有任何反应的
         */
        LogUtils.d("HockeyRenderer onSurfaceChanged() gl = " + gl + "  width = " + width + "  height = " + height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        /**
         *  当绘制一帧时，这个方法会被调用。在这个方法中，我们一定要绘制一些东西，
         * 即使只是清空屏幕；因为在这方法返回之后，渲染缓冲区会被交换（前人的源码分析），
         * 并显示在屏幕上，如果什么都没画，可能会看到糟糕的闪烁效果。
         *  我们调用 GLES20.glClear(GL_COLOR_BUFFER_BIT);
         *  清空屏幕，这会擦除屏幕上的所有颜色，并用之前glClearColor调用定义的颜色填充整个屏幕。
         */
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }
}
