package com.cs.openglapplication

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log

class MyGLRenderer() : GLSurfaceView.Renderer {
    lateinit var mTriangle: Triangle
    lateinit var mSquare: Square2

    private val projectionMatrix = FloatArray(16)   // 视图位置
    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private val vPMatrix = FloatArray(16)           // projectionMatrix x viewMatrix
    private val viewMatrix = FloatArray(16)         // camera 位置

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        // initialize a triangle
        mTriangle = Triangle()
        // initialize a square
        mSquare = Square2()
    }
    private val rotationMatrix = FloatArray(16)

    override fun onDrawFrame(unused: GL10) {
        val scratch = FloatArray(16)
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)


        // Create a rotation transformation for the triangle
        val time = SystemClock.uptimeMillis() % 4000L
        val angle = 0.090f * time.toInt()
        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 0f, -1.0f)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        // Draw triangle
        mTriangle.draw(scratch)
    }



    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // Matrix.frustumM() 是 Android 中 Matrix 类的一个静态方法，用于生成透视投影矩阵，
        // 主要用于 3D 图形开发（如 OpenGL ES）中定义视锥体（Viewing Frustum），将 3D 场景投影到 2D 屏幕上。
        // public static void frustumM(
        //     float[] m,      // 目标矩阵数组（用于存储生成的投影矩阵）
        //     int mOffset,    // 矩阵在数组中的起始偏移量（通常传 0，表示从数组第一个元素开始存储）
        //     float left,     // 视锥体左边界在 X 轴的坐标（近裁剪面的左边缘）
        //     float right,    // 视锥体右边界在 X 轴的坐标（近裁剪面的右边缘）
        //     float bottom,   // 视锥体下边界在 Y 轴的坐标（近裁剪面的下边缘）
        //     float top,      // 视锥体上边界在 Y 轴的坐标（近裁剪面的上边缘）
        //     float near,     // 近裁剪面在 Z 轴的距离（必须 > 0）
        //     float far       // 远裁剪面在 Z 轴的距离（必须 > near）
        // )
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }
}