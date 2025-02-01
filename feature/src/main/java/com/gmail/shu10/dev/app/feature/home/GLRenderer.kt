package com.gmail.shu10.dev.app.feature.home

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * OpenGL ES を用いて Bitmap をテクスチャとしてレンダリングするクラス
 */
class GLRenderer(private val width: Int, private val height: Int) {

    // シンプルな頂点・フラグメントシェーダー
    private val vertexShaderCode =
        """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        uniform mat4 uMVPMatrix;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vTexCoord = aTexCoord;
        }
        """.trimIndent()

    private val fragmentShaderCode =
        """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
        """.trimIndent()

    // 四角形（全画面）描画用の頂点データ（X,Y,Z と U,V）
    private val vertexData = floatArrayOf(
        //  X,     Y,    Z,    U,  V
        -1f, -1f, 0f,  0f, 0f,   // 左下
        1f, -1f, 0f,   1f, 0f,   // 右下
        -1f, 1f, 0f,   0f, 1f,   // 左上
        1f, 1f, 0f,    1f, 1f    // 右上
    )

    // 頂点インデックス（2つの三角形）
    private val indexData = shortArrayOf(0, 1, 2, 2, 1, 3)

    private var program = 0
    private var aPositionHandle = 0
    private var aTexCoordHandle = 0
    private var uMVPMatrixHandle = 0
    private var uTextureHandle = 0

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertexData.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(vertexData)
            position(0)
        }
    private val indexBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(indexData.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
            put(indexData)
            position(0)
        }

    // テクスチャハンドル
    var textureId: Int = -1

    init {
        initGL()
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        // シェーダーコンパイルのエラーチェックは省略
        return shader
    }

    private fun initGL() {
        // シェーダーの生成
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // プログラムの作成
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        // ハンドルの取得
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uTextureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        // テクスチャ生成
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * Bitmap をテクスチャに転送し、四角形へ描画する
     */
    fun render(bitmap: Bitmap) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        // MVP行列は単位行列（必要に応じて反転や回転を入れる）
        val mvpMatrix = FloatArray(16)
        Matrix.setIdentityM(mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)

        // テクスチャのバインド＆Bitmap転送
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        // Bitmap の内容をテクスチャへアップロード
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glUniform1i(uTextureHandle, 0)

        // 頂点属性の設定
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 5 * 4, vertexBuffer)

        vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)
        GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 5 * 4, vertexBuffer)

        // インデックスバッファを用いて描画
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexData.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // 後片付け
        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}
