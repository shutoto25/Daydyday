package com.gmail.shu10.dev.app.feature.home

import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.min

/**
 * GLRenderer は、固定出力解像度 1920×1920 の正方形内に、
 * 入力 Bitmap をアスペクト比を維持して中央配置（レターボックス）し描画する。
 */
class GLRenderer(private val width: Int, private val height: Int) {

    // 頂点シェーダーとフラグメントシェーダーのコード
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        uniform mat4 uMVPMatrix;
        void main(){
            gl_Position = uMVPMatrix * aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main(){
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    // 頂点インデックス（四角形を2つの三角形で表現）
    private val indexData = shortArrayOf(0, 1, 2, 2, 1, 3)
    private val indexBuffer: ShortBuffer = ByteBuffer.allocateDirect(indexData.size * 2)
        .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
            put(indexData)
            position(0)
        }

    // 頂点データは動的に生成するので、十分なサイズのFloatBufferを確保
    private var vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(4 * 5 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var program: Int = 0
    private var aPositionHandle: Int = 0
    private var aTexCoordHandle: Int = 0
    private var uMVPMatrixHandle: Int = 0
    private var uTextureHandle: Int = 0

    // 投影行列（オーソグラフィック）
    private val projectionMatrix = FloatArray(16)

    // テクスチャハンドル
    var textureId: Int = -1

    init {
        initGL()
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun initGL() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        // viewportの設定
        GLES20.glViewport(0, 0, width, height)
        // オーソグラフィック投影行列を作成（左=0, 右=targetSize, 下=0, 上=targetSize）
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uTextureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * 入力 Bitmap をテクスチャに転送し、固定出力 1920×1920 の正方形内に、
     * 入力画像のアスペクト比を維持して中央に配置（レターボックス）して描画する。
     * rotationDegrees が0以外の場合は、画像中心を軸に回転する。
     */
    fun render(bitmap: Bitmap, rotationDegrees: Float = 0f) {
        // 背景を黒でクリア
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glViewport(0, 0, width, height)

        // 入力画像のサイズ（ピクセル）
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()
        // アスペクト比を維持して、画像全体が収まるようにフィットするスケールを計算
        val scale = min(width / imageWidth, height / imageHeight)
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        // レターボックス配置用：余白分のオフセット（中央配置）
        val offsetX = (width - scaledWidth) / 2f
        val offsetY = (height - scaledHeight) / 2f

        // 以下、描画矩形の各頂点座標（ピクセル単位: 0〜targetSize）を計算
        var blX = offsetX;
        var blY = offsetY                   // bottom-left
        var brX = offsetX + scaledWidth;
        var brY = offsetY       // bottom-right
        var tlX = offsetX;
        var tlY = offsetY + scaledHeight      // top-left
        var trX = offsetX + scaledWidth;
        var trY = offsetY + scaledHeight // top-right

        // 必要なら回転を適用（画像の中心を軸に回転）
        if (rotationDegrees != 0f) {
            val centerX = offsetX + scaledWidth / 2f
            val centerY = offsetY + scaledHeight / 2f
            fun rotatePoint(x: Float, y: Float): Pair<Float, Float> {
                val dx = x - centerX
                val dy = y - centerY
                val rad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
                val cos = Math.cos(rad.toDouble()).toFloat()
                val sin = Math.sin(rad.toDouble()).toFloat()
                val rx = dx * cos - dy * sin + centerX
                val ry = dx * sin + dy * cos + centerY
                return Pair(rx, ry)
            }

            val bl = rotatePoint(blX, blY)
            val br = rotatePoint(brX, brY)
            val tl = rotatePoint(tlX, tlY)
            val tr = rotatePoint(trX, trY)
            blX = bl.first; blY = bl.second
            brX = br.first; brY = br.second
            tlX = tl.first; tlY = tl.second
            trX = tr.first; trY = tr.second
        }

        // 頂点データ生成：各頂点は (x, y, z, u, v)
        val vertices = floatArrayOf(
            blX, blY, 0f, 0f, 0f,    // bottom-left: テクスチャ座標 (0,0)
            brX, brY, 0f, 1f, 0f,    // bottom-right: (1,0)
            tlX, tlY, 0f, 0f, 1f,    // top-left: (0,1)
            trX, trY, 0f, 1f, 1f     // top-right: (1,1)
        )
        vertexBuffer.clear()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // MVP 行列として、projectionMatrix をそのまま使用（すでに 0〜targetSize の座標系に写像済み）
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0)

        // Bitmap をテクスチャへアップロード
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glUniform1i(uTextureHandle, 0)

        // 頂点属性の設定および描画
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(
            aPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            5 * 4,
            vertexBuffer
        )
        vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)
        GLES20.glVertexAttribPointer(
            aTexCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            5 * 4,
            vertexBuffer
        )
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indexData.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    fun renderFrameFromDecoder(
        decoderTextureId: Int,
        transformMatrix: FloatArray,
        targetSize: Int,
    ) {
        // ① 背景を黒でクリア
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glViewport(0, 0, targetSize, targetSize)

        // ② MVP 行列の生成は、ここでは単純に既存の projectionMatrix と transformMatrix を合成する例です。
        val mvpMatrix = FloatArray(16)
        // ここでは、transformMatrix は decoderSurfaceTexture から取得した変換行列
        // 合成方法は実装次第ですが、ここでは単純に projectionMatrix * transformMatrix としています。
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, transformMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)

        // ③ decoderTextureId は GL_TEXTURE_EXTERNAL_OES ターゲットにバインド
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, decoderTextureId)

        // ④ 頂点属性の設定と描画
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(
            aPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            5 * 4,
            vertexBuffer
        )
        vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)
        GLES20.glVertexAttribPointer(
            aTexCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            5 * 4,
            vertexBuffer
        )
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indexData.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }
}