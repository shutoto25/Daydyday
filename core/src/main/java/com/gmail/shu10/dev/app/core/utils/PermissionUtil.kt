package com.gmail.shu10.dev.app.core.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 権限があるかどうかをチェック
 * @param context コンテキスト
 * @param permission 権限チェックをしたいパーミッション名
 * @return 権限がある場合はtrue
 */
fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

