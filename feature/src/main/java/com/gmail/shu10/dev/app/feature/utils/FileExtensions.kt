package com.gmail.shu10.dev.app.feature.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * FileをFileProviderを利用したContent URIに変換
 * @param context コンテキスト
 * @return Content URI
 */
fun File.toContentUri(context: Context): Uri {
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        this
    )
}