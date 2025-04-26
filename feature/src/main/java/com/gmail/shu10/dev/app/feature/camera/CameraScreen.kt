package com.gmail.shu10.dev.app.feature.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.gmail.shu10.dev.app.core.utils.hasPermission
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * カメラを起動するためのComposable関数
 * @param context Context
 * @param onPhotoTaken 写真撮影後のコールバック
 */
@Composable
fun rememberCameraLauncher(
    context: Context,
    onPhotoTaken: (Uri) -> Unit
): Pair<() -> Unit, ActivityResultLauncher<Uri>> {
    // 撮影した写真のURIを保持する状態
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // カメラアプリ起動用のActivityResultLauncher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            // 写真撮影成功
            onPhotoTaken(photoUri!!)
        }
    }

    // 権限リクエスト用のActivityResultLauncher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 権限が許可されたらカメラを起動
            launchCamera(context, cameraLauncher) { uri ->
                photoUri = uri
            }
        }
    }

    // カメラ起動処理をラップした関数
    val launchCameraWithPermissionCheck: () -> Unit = {
        // カメラ権限があるか確認
        if (hasPermission(context, Manifest.permission.CAMERA)) {
            // 権限があればカメラ起動
            launchCamera(context, cameraLauncher) { uri ->
                photoUri = uri
            }
        } else {
            // 権限がなければリクエスト
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    return Pair(launchCameraWithPermissionCheck, cameraLauncher)
}

/**
 * カメラを起動する
 * @param context Context
 * @param cameraLauncher カメラ起動用のActivityResultLauncher
 * @param onUriCreated 写真URIが作成されたときのコールバック
 */
private fun launchCamera(
    context: Context,
    cameraLauncher: ActivityResultLauncher<Uri>,
    onUriCreated: (Uri) -> Unit
) {
    try {
        // 一時ファイルの作成
        val photoFile = createImageFile(context)
        val photoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        // URIをコールバックで通知
        onUriCreated(photoUri)

        // カメラアプリの起動
        cameraLauncher.launch(photoUri)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 写真保存用の一時ファイルを作成
 * @param context Context
 * @return 作成された空のファイル
 */
private fun createImageFile(context: Context): File {
    // ファイル名に現在日時を使用
    val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val fileName = "JPEG_${timeStamp}_"

    // アプリの外部キャッシュディレクトリに一時ファイルを作成
    val storageDir = context.getExternalFilesDir(null)
    return File.createTempFile(fileName, ".jpg", storageDir)
}