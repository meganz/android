package mega.privacy.android.app.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.view.OrientationEventListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mega.privacy.android.app.camera.menu.CameraMenuAction
import mega.privacy.android.app.camera.model.CameraOption
import mega.privacy.android.app.camera.state.CamSelector
import mega.privacy.android.app.camera.state.FlashMode
import mega.privacy.android.app.camera.state.rememberCameraState
import mega.privacy.android.app.camera.state.rememberSaveableCamSelector
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.camera.CameraBottomAppBar
import mega.privacy.android.shared.original.core.ui.controls.camera.CameraTimer
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.utils.rememberPermissionState
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun CameraCaptureScreen(
    onOpenVideoPreview: (Uri) -> Unit,
    onOpenPhotoPreview: (Uri) -> Unit,
    onFinish: (uri: Uri?) -> Unit = {},
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val cameraState = rememberCameraState()
    var flashMode by rememberSaveable { mutableStateOf(cameraState.flashMode) }
    var camSelector by rememberSaveableCamSelector(CamSelector.Back)
    var cameraOption by rememberSaveable { mutableStateOf(CameraOption.Photo) }
    val isRecording by rememberUpdatedState(cameraState.isRecording)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val uiState by viewModel.state.collectAsStateWithLifecycle()

    var startTimerInMillis by rememberSaveable { mutableLongStateOf(0L) }
    var countTimerInMillis by rememberSaveable { mutableLongStateOf(0L) }

    val durationInSecondsTextMapper = remember {
        DurationInSecondsTextMapper()
    }

    var selectFlashMode by rememberSaveable { mutableStateOf(false) }
    var rotation by rememberSaveable { mutableIntStateOf(0) }
    val animatedRotation = rememberAnimationRotation(rotation.toFloat())

    val flashOptions = remember {
        hashMapOf(
            FlashMode.Auto to CameraMenuAction.FlashAuto(),
            FlashMode.On to CameraMenuAction.FlashOn(),
            FlashMode.Off to CameraMenuAction.FlashOff()
        )
    }

    val galleryPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) {
            it?.let {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onFinish(it)
            }
        }

    LaunchedEffect(isRecording) {
        startTimerInMillis = System.currentTimeMillis()
        while (isRecording && isActive) {
            countTimerInMillis = System.currentTimeMillis() - startTimerInMillis
            delay(1.seconds)
        }

        countTimerInMillis = 0L
        startTimerInMillis = 0L
    }

    EventEffect(
        event = uiState.onCapturePhotoEvent,
        onConsumed = viewModel::onTakePictureEventConsumed
    ) { uri ->
        uri?.let { onOpenPhotoPreview(it) }
    }

    EventEffect(
        event = uiState.onCaptureVideoEvent,
        onConsumed = viewModel::onCaptureVideoEventConsumed
    ) { uri ->
        uri?.let { onOpenVideoPreview(it) }
    }

    DisposableEffect(Unit) {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                // https://developer.android.com/media/camera/camerax/orientation-rotation#orientation-event-listener-setup
                rotation = when (orientation) {
                    in 45 until 135 -> 270
                    in 135 until 225 -> 180
                    in 225 until 315 -> 90
                    else -> 0
                }
            }
        }

        orientationEventListener.enable()
        onDispose {
            orientationEventListener.disable()
        }
    }

    // force dark mode
    MegaScaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = "",
                onNavigationPressed = {
                    onFinish(null)
                },
                actions = if (selectFlashMode) {
                    flashOptions.values.toList()
                } else {
                    flashOptions[flashMode]?.let { listOf(it) }.orEmpty()
                }.takeIf { !isRecording && camSelector == CamSelector.Back }, // hide flash options when recording and front camera is selected
                onActionPressed = {
                    if (selectFlashMode) {
                        flashMode = when (it) {
                            is CameraMenuAction.FlashAuto -> FlashMode.Auto
                            is CameraMenuAction.FlashOn -> FlashMode.On
                            is CameraMenuAction.FlashOff -> FlashMode.Off
                            else -> flashMode
                        }
                    }
                    selectFlashMode = !selectFlashMode
                }
            )
        },
        bottomBar = {
            CameraBottomAppBar(
                rotationDegree = animatedRotation,
                isCaptureVideo = cameraOption == CameraOption.Video,
                isRecording = isRecording,
                onToggleCamera = {
                    camSelector = camSelector.inverse
                },
                onToggleCaptureMode = {
                    cameraOption =
                        if (cameraOption == CameraOption.Photo) CameraOption.Video else CameraOption.Photo
                },
                onCameraAction = {
                    if (cameraOption == CameraOption.Photo) {
                        viewModel.takePicture(cameraState)
                    } else {
                        viewModel.captureVideo(cameraState, audioPermissionState.status.isGranted)
                    }
                },
                onOpenGallery = {
                    galleryPicker.launch(PickVisualMediaRequest())
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding),
        ) {
            CameraPreview(
                cameraState = cameraState,
                camSelector = camSelector,
                captureMode = cameraOption.mode,
                flashMode = flashMode,
            )
            if (isRecording) {
                CameraTimer(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    formattedTime = durationInSecondsTextMapper(countTimerInMillis.milliseconds),
                )
            }
        }
    }
}

@Composable
private fun rememberAnimationRotation(rotation: Float): Float {
    val targetRotation by animateFloatAsState(
        targetValue = if (rotation > 360 - rotation) {
            -(360 - rotation)
        } else {
            rotation
        },
        animationSpec = tween(500),
        label = "rotation",
    )
    return targetRotation
}