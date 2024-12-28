package com.gmail.shu10.dev.app.core.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 権限があるかどうか
 * @param context Context
 * @param permission 権限チェックをしたいパーミッション名
 */
fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED

