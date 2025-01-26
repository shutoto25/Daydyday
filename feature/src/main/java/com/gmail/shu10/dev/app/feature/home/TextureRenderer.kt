import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextureRenderer(private val width: Int, private val height: Int, bitmap: Bitmap) {
    private val isPortrait = height > width
    private val isBitmapPortrait = bitmap.height > bitmap.width  // **画像が縦向きかどうかを判定**

    private val vertexShaderCode =
        """
        attribute vec4 a_Position;
        attribute vec2 a_TexCoord;
        varying vec2 v_TexCoord;
        void main() {
            gl_Position = a_Position;
            v_TexCoord = a_TexCoord;
        }
        """.trimIndent()

    private val fragmentShaderCode =
        """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main() {
            gl_FragColor = texture2D(u_Texture, v_TexCoord);
        }
        """.trimIndent()

    private val vertexData: FloatArray
    private val texCoordData: FloatArray
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    private var program: Int = 0
    private var textureId = 0

    init {
        // **1️⃣ OpenGL の描画範囲を設定**
        if (isPortrait != isBitmapPortrait) {
            GLES20.glViewport(0, 0, height, width) // **画像と動画の向きが異なる場合、入れ替える**
        } else {
            GLES20.glViewport(0, 0, width, height)
        }
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // **2️⃣ 画像の回転を修正**
        val correctedBitmap = if (isPortrait == isBitmapPortrait) {
            bitmap  // **向きが合っていればそのまま**
        } else {
            rotateBitmap(bitmap, 90f)  // **向きが異なれば 90° 回転**
        }

        val bitmapRatio = correctedBitmap.width.toFloat() / correctedBitmap.height
        val screenRatio = width.toFloat() / height

        val scaleX: Float
        val scaleY: Float

        if (bitmapRatio > screenRatio) {
            scaleX = screenRatio / bitmapRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = bitmapRatio / screenRatio
        }

        // **3️⃣ 頂点データの設定**
        vertexData = floatArrayOf(
            -scaleX, -scaleY,  // 左下
            scaleX, -scaleY,   // 右下
            -scaleX, scaleY,   // 左上
            scaleX, scaleY     // 右上
        )

        // **4️⃣ テクスチャ座標の修正（回転に応じて適用）**
        texCoordData = floatArrayOf(
            0f, 1f,  // 左下
            1f, 1f,  // 右下
            0f, 0f,  // 左上
            1f, 0f   // 右上
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexData)
                position(0)
            }

        texCoordBuffer = ByteBuffer.allocateDirect(texCoordData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(texCoordData)
                position(0)
            }

        program = createProgram(vertexShaderCode, fragmentShaderCode)
        textureId = createTexture(correctedBitmap)
    }

    fun drawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        val positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord")
        val textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun createProgram(vertexShader: String, fragmentShader: String): Int {
        val vertexShaderId = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vertexShaderId, vertexShader)
        GLES20.glCompileShader(vertexShaderId)

        val fragmentShaderId = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fragmentShaderId, fragmentShader)
        GLES20.glCompileShader(fragmentShaderId)

        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShaderId)
        GLES20.glAttachShader(programId, fragmentShaderId)
        GLES20.glLinkProgram(programId)
        return programId
    }

    private fun createTexture(bitmap: Bitmap): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        if (textureIds[0] == 0) {
            throw RuntimeException("Failed to generate a texture ID")
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        return textureIds[0]
    }

    // **画像の回転処理**
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}