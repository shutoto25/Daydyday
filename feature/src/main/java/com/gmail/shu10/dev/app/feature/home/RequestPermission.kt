package com.gmail.shu10.dev.app.feature.home

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.glance.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gmail.shu10.dev.app.core.utils.hasPermission

/**
 * 権限リクエスト
 */
@Composable
fun RequestPermission(permission: String) {

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            Log.d("TAG", " $permission isGranted = $isGranted")
        }
    )
    // Permissionが許可されていなければActivityがonStartに遷移したときに
    // Launcherを利用してPermissionRequestを実行するLifecycleObserverを作成
    val lifecycleObserver = remember {
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (!hasPermission(context, permission)) {
                    launcher.launch(permission)
                }
            }
        }
    }
    DisposableEffect(lifecycle, lifecycleObserver) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
}