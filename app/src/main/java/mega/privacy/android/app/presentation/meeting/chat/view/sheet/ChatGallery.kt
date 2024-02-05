package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPartialMediaPermission
import mega.privacy.android.core.ui.controls.chat.attachpanel.AskGalleryPermissionView
import mega.privacy.android.core.ui.controls.chat.attachpanel.ChatGalleryItem
import mega.privacy.android.core.ui.controls.chat.attachpanel.PartialPermissionView
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Chat gallery
 *
 * @param sheetState Bottom sheet state
 * @param modifier Modifier
 * @param onTakePicture Callback when take picture
 * @param onCameraPermissionDenied Callback when camera permission denied
 * @param viewModel View model
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ColumnScope.ChatGallery(
    sheetState: ModalBottomSheetState,
    modifier: Modifier = Modifier,
    onTakePicture: () -> Unit = {},
    onCameraPermissionDenied: () -> Unit = {},
    viewModel: ChatGalleryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionState =
        rememberMultiplePermissionsState(permissions = getMediaPermissions().toList())
    val isMediaPermissionGranted = permissionState.permissions.any { it.status.isGranted }
    var shouldReloadGallery by remember { mutableStateOf(false) }
    // workaround to handle permission denied and navigate to app settings
    var requestingPermissionTimestamp by remember { mutableLongStateOf(0L) }
    val mediaPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // in case user re-selects media files, we need to reload the gallery
        shouldReloadGallery = hasPartialMediaPermission(context)
        // if the result returns immediately it means asking dialog not shown, so we should navigate to app settings
        if (it.values.all { isGrant -> !isGrant } && (System.currentTimeMillis() - requestingPermissionTimestamp < 500L)) {
            context.navigateToAppSettings()
        }
    }

    LaunchedEffect(isMediaPermissionGranted, sheetState.isVisible) {
        if (isMediaPermissionGranted && sheetState.isVisible) {
            viewModel.loadGalleryImages()
        } else if (!sheetState.isVisible) {
            viewModel.removeGalleryImages()
        }
    }

    LaunchedEffect(shouldReloadGallery) {
        if (shouldReloadGallery) {
            viewModel.loadGalleryImages()
        }
        shouldReloadGallery = false
    }

    ChatGalleryContent(
        modifier = modifier,
        onTakePicture = onTakePicture,
        onCameraPermissionDenied = onCameraPermissionDenied,
        sheetState = sheetState,
        isMediaPermissionGranted = isMediaPermissionGranted,
        onRequestMediaPermission = {
            requestingPermissionTimestamp = System.currentTimeMillis()
            mediaPermissionsLauncher.launch(getMediaPermissions().toTypedArray())
        },
        images = state.items,
        isLoading = state.isLoading
    )
}

/**
 * Chat gallery content
 *
 * @param modifier
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatGalleryContent(
    sheetState: ModalBottomSheetState,
    isMediaPermissionGranted: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    images: List<FileGalleryItem> = emptyList(),
    onTakePicture: () -> Unit = {},
    onCameraPermissionDenied: () -> Unit = {},
    onRequestMediaPermission: () -> Unit = {},
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    LazyRow(
        modifier = modifier.testTag(TEST_TAG_GALLERY_LIST),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item("camera_button") {
            val view = LocalView.current
            if (!view.isInEditMode) {
                ChatCameraButton(
                    modifier = Modifier.size(88.dp),
                    sheetState = sheetState,
                    onTakePicture = onTakePicture,
                    onCameraPermissionDenied = onCameraPermissionDenied,
                )
            } else {
                ChatGalleryItem(modifier = Modifier.size(88.dp)) {}
            }
        }

        if (!isMediaPermissionGranted) {
            item("gallery_permission_view") {
                AskGalleryPermissionView(
                    modifier = Modifier
                        .height(88.dp)
                        .width(screenWidth.dp - 88.dp - 3.times(4.dp)),
                    onRequestPermission = onRequestMediaPermission
                )
            }
        } else {
            if (hasPartialMediaPermission(context)) {
                item("partial_permission_view") {
                    PartialPermissionView(
                        modifier = Modifier.size(88.dp),
                        onRequestPermission = onRequestMediaPermission
                    )
                }
            }
            if (images.isNotEmpty()) {
                items(images) { item ->
                    ChatGalleryItem(
                        modifier = Modifier.size(88.dp),
                    ) {
                        key(item.id) {
                            if (item.isImage) {
                                AsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    model = item.fileUri,
                                    contentDescription = "Image",
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        modifier = Modifier.fillMaxSize(),
                                        model = ImageRequest.Builder(context)
                                            .data(item.fileUri)
                                            .decoderFactory { result, options, _ ->
                                                VideoFrameDecoder(
                                                    result.source,
                                                    options
                                                )
                                            }.build(),
                                        contentDescription = "Video",
                                        contentScale = ContentScale.Crop
                                    )

                                    MegaText(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .background(
                                                color = Color.Black.copy(alpha = 0.7f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(4.dp),
                                        text = item.duration.orEmpty(),
                                        textColor = TextColor.OnColor,
                                        style = MaterialTheme.typography.body2.copy(fontSize = 10.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isLoading) {
        MegaLinearProgressIndicator(
            modifier = Modifier
                .testTag(TEST_TAG_LOADING_GALLERY)
                .padding(horizontal = 4.dp)
        )
    } else {
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun getMediaPermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        mutableListOf(
            PermissionUtils.getImagePermissionByVersion(),
            PermissionUtils.getVideoPermissionByVersion(),
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(PermissionUtils.getPartialMediaPermission())
            }
        }
    } else {
        listOf(
            PermissionUtils.getReadExternalStoragePermission()
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun ChatGalleryContentPreview(
    @PreviewParameter(BooleanProvider::class) isMediaPermissionGranted: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatGalleryContent(
            sheetState = ModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                isSkipHalfExpanded = false,
                density = LocalDensity.current,
            ),
            isMediaPermissionGranted = isMediaPermissionGranted,
            images = emptyList(),
        )
    }
}