import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextureRenderer(private val width: Int, private val height: Int, private val bitmap: Bitmap) {
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
    private val vertexBuffer: FloatBuffer

    private var program: Int = 0
    private var textureId = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0

    init {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // **アスペクト比補正**
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height
        val screenRatio = width.toFloat() / height

        val scaleX: Float
        val scaleY: Float

        if (bitmapRatio > screenRatio) {
            scaleX = 1f
            scaleY = screenRatio / bitmapRatio
        } else {
            scaleX = bitmapRatio / screenRatio
            scaleY = 1f
        }

        vertexData = floatArrayOf(
            -scaleX, -scaleY, 0f, 1f,
            scaleX, -scaleY, 1f, 1f,
            -scaleX, scaleY, 0f, 0f,
            scaleX, scaleY, 1f, 0f
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexData)
                position(0)
            }

        program = createProgram(vertexShaderCode, fragmentShaderCode)

        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")

        textureId = createTexture()
    }

    fun drawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(program)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        vertexBuffer.position(2)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun createProgram(vertexShader: String, fragmentShader: String): Int {
        val vertexShaderId = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderId = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShaderId)
        GLES20.glAttachShader(programId, fragmentShaderId)
        GLES20.glLinkProgram(programId)

        return programId
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textureIds[0]
    }
}