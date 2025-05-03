package com.cs.openglapplication


import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLES20
import android.opengl.GLUtils
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLTimeRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var program: Int = 0
    private var positionHandle: Int = 0
    private var textureCoordHandle: Int = 0
    private var textureHandle: Int = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var textureBuffer: FloatBuffer
    private var textureId: Int = 0
    private var viewportWidth: Int = 0
    private var viewportHeight: Int = 0
    private companion object {
        private const val COORDS_PER_VERTEX = 2
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4
        private const val COORDS_PER_TEXTURE = 2
        private const val TEXTURE_STRIDE = COORDS_PER_TEXTURE * 4
    }
    private var vertices = FloatArray(8)
    private val textureCoords = floatArrayOf(
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    )

    init {
        val tb = ByteBuffer.allocateDirect(textureCoords.size * 4)
        tb.order(ByteOrder.nativeOrder())
        textureBuffer = tb.asFloatBuffer()
        textureBuffer.put(textureCoords)
        textureBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        val vertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec2 a_texCoord;
            varying vec2 v_texCoord;
            void main() {
              gl_Position = vPosition;
              v_texCoord = a_texCoord;
            }
        """.trimIndent()
        val fragmentShaderCode = """
            precision mediump float;
            uniform sampler2D u_texture;
            varying vec2 v_texCoord;
            void main() {
              gl_FragColor = texture2D(u_texture, v_texCoord);
            }
        """.trimIndent()
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // 生成纹理
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // 更新时间纹理
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val bitmap = createTextBitmap(time)
        updateVertices(bitmap.width, bitmap.height)
        updateTexture(bitmap)

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer)

        textureCoordHandle = GLES20.glGetAttribLocation(program, "a_texCoord")
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        GLES20.glVertexAttribPointer(textureCoordHandle, COORDS_PER_TEXTURE, GLES20.GL_FLOAT, false, TEXTURE_STRIDE, textureBuffer)

        textureHandle = GLES20.glGetUniformLocation(program, "u_texture")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createTextBitmap(text: String): Bitmap {
        val paint = Paint()
        paint.color = android.graphics.Color.WHITE
        // 进一步减小字体大小
        paint.textSize = 48f
        val width = paint.measureText(text).toInt()
        val height = paint.fontMetricsInt.bottom - paint.fontMetricsInt.top
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED)
        canvas.drawText(text, 0f, (-paint.fontMetricsInt.ascent).toFloat(), paint)
        return bitmap
    }

    private fun updateTexture(bitmap: Bitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
    }

    private fun updateVertices(bitmapWidth: Int, bitmapHeight: Int) {
        val aspectRatio = viewportWidth.toFloat() / viewportHeight.toFloat()
        val textAspectRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        // 减小缩放比例
        val scaleX = 0.2f
        val scaleY = scaleX / (aspectRatio * textAspectRatio)
        vertices = floatArrayOf(
            -1.0f,  1.0f,
            -1.0f + scaleX,  1.0f,
            -1.0f,  1.0f - scaleY,
            -1.0f + scaleX,  1.0f - scaleY
        )
        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)
    }
}