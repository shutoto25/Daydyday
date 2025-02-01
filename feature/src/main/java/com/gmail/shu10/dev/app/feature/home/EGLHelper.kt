package com.gmail.shu10.dev.app.feature.home

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLUtils
import android.view.Surface

/**
 * EGL/OpenGL ES の初期化および描画を行うクラス
 */
class EGLHelper(private val surface: Surface, private val width: Int, private val height: Int) {

    val eglDisplay: EGLDisplay
    val eglContext: EGLContext
    val eglSurface: EGLSurface

    init {
        // EGLディスプレイの取得と初期化
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("Unable to initialize EGL14")
        }

        // EGLコンフィグの選択
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGLExt.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)) {
            throw RuntimeException("Unable to choose EGL config")
        }
        val eglConfig = configs[0] ?: throw RuntimeException("No EGL config found")

        // EGLコンテキストの作成
        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        if (eglContext == null) {
            throw RuntimeException("Failed to create EGL context")
        }

        // EGLサーフェスの作成（MediaCodecの入力Surfaceを使用）
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0)
        if (eglSurface == null) {
            throw RuntimeException("Failed to create EGL window surface")
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError()))
        }
    }

    /**
     * フレームをレンダリング後、タイムスタンプをセットしてバッファをスワップ
     */
    fun swapBuffers(presentationTimeNs: Long) {
        // タイムスタンプ設定（単位：ナノ秒）
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, presentationTimeNs)
        if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
            throw RuntimeException("eglSwapBuffers failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError()))
        }
    }

    /**
     * EGLリソースの解放
     */
    fun release() {
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(eglDisplay)
    }

    companion object {
        fun checkEglError(msg: String) {
            val error = EGL14.eglGetError()
            if (error != EGL14.EGL_SUCCESS) {
                throw RuntimeException("$msg: EGL error: 0x${Integer.toHexString(error)}")
            }
        }
    }
}
