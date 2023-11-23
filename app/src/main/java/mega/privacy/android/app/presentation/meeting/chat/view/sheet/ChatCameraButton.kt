package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import mega.privacy.android.app.R
import mega.privacy.android.core.theme.tokens.MegaAppTheme
import mega.privacy.android.core.ui.controls.chat.ChatGalleryItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import timber.log.Timber

internal const val TEST_TAG_CHAT_CAMERA_BUTTON = "chat_camera_button"
internal const val TEST_TAG_CHAT_CAMERA_BUTTON_ICON = "chat_camera_button:icon"

/**
 * Chat camera button
 *
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatCameraButton(
    sheetState: ModalBottomSheetState,
    modifier: Modifier = Modifier,
) {
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController =
        remember { LifecycleCameraController(context) }
    val cameraPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraController.bindToLifecycle(lifecycleOwner)
        }
    }
    var isFrameReady by remember { mutableStateOf(false) }
    DisposableEffect(sheetState.isVisible) {
        if (sheetState.isVisible) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                cameraPermissionsLauncher.launch(Manifest.permission.CAMERA)
            } else {
                cameraController.bindToLifecycle(lifecycleOwner)
            }
        } else {
            cameraController.unbind()
        }
        onDispose { }
    }
    Box(
        modifier = modifier
            .testTag(TEST_TAG_CHAT_CAMERA_BUTTON)
            .clickable {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    cameraPermissionsLauncher.launch(Manifest.permission.CAMERA)
                }
            },
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = LocalContentColor.current,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp)),
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_START
                    setBackgroundColor(Color.TRANSPARENT)
                }.also { previewView ->
                    previewView.controller = cameraController
                    previewView.previewStreamState.observe(lifecycleOwner) { state ->
                        Timber.d("Preview stream state: $state")
                        isFrameReady = state == PreviewView.StreamState.STREAMING
                    }
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
        )

        if (!isFrameReady) {
            // when frame is not ready show place holder
            ChatGalleryItem(modifier = Modifier.fillMaxSize()) {}
        }

        Image(
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(TEST_TAG_CHAT_CAMERA_BUTTON_ICON),
            painter = painterResource(id = R.drawable.ic_take_photo),
            contentDescription = "Take photo"
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun ChatCameraButtonPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatCameraButton(
            modifier = Modifier.size(88.dp),
            sheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Hidden
            )
        )
    }
}