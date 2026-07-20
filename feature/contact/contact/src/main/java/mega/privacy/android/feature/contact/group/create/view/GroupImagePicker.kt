package mega.privacy.android.feature.contact.group.create.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import mega.android.core.ui.components.button.PrimaryLargeIconButton
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.icon.pack.IconPack
import java.io.File
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Group-chat image picker: a circular button that shows the currently chosen [imageUri] (or a camera
 * glyph when none is set) and, on tap, opens a chooser to take a photo or pick one from the gallery.
 * The picked image is reported through [onImagePicked].
 *
 * Camera capture writes to a temp file in the app cache exposed via the app FileProvider; the gallery
 * path uses the system photo picker ([ActivityResultContracts.PickVisualMedia]) which needs no runtime
 * permission.
 *
 * @param imageUri the currently selected image, or null when none is chosen.
 * @param onImagePicked invoked with the newly picked image URI.
 * @param modifier
 */
@Composable
internal fun GroupImagePicker(
    imageUri: Uri?,
    onImagePicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showChooser by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onImagePicked) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> if (success) pendingCameraUri?.let(onImagePicked) }

    val launchCamera = {
        val uri = createGroupImageUri(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) launchCamera() }

    GroupImagePickerButton(
        imageUri = imageUri,
        onClick = { showChooser = true },
        modifier = modifier,
    )

    if (showChooser) {
        GroupImageSourceBottomSheet(
            onDismiss = { showChooser = false },
            onTakePhoto = {
                showChooser = false
                if (hasCameraPermission(context)) {
                    launchCamera()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onChooseFromGallery = {
                showChooser = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

@Composable
internal fun GroupImagePickerButton(
    imageUri: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(sharedR.string.create_group_chat_image_content_description)
    if (imageUri == null) {
        PrimaryLargeIconButton(
            modifier = modifier
                .clip(CircleShape)
                .testTag(GROUP_IMAGE_PICKER_BUTTON_TAG),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Camera),
            onClick = onClick,
            contentDescription = contentDescription,
        )
    } else {
        val spacing = LocalSpacing.current
        AsyncImage(
            model = imageUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(spacing.x56)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .testTag(GROUP_IMAGE_PICKER_BUTTON_TAG),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupImageSourceBottomSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val close = { action: () -> Unit ->
        coroutineScope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
                onDismiss()
                action()
            }
    }

    MegaModalBottomSheet(
        modifier = Modifier.testTag(GROUP_IMAGE_SOURCE_SHEET_TAG),
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        GroupImageSourceSheetContent(
            onTakePhoto = { close(onTakePhoto) },
            onChooseFromGallery = { close(onChooseFromGallery) },
        )
    }
}

@Composable
internal fun GroupImageSourceSheetContent(
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
) {
    Column {
        NodeActionListTile(
            text = stringResource(sharedR.string.create_group_chat_image_take_photo),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Camera),
            onActionClicked = onTakePhoto,
            modifier = Modifier.testTag(GROUP_IMAGE_SOURCE_TAKE_PHOTO_TAG),
        )
        NodeActionListTile(
            text = stringResource(sharedR.string.create_group_chat_image_choose_from_gallery),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Image01),
            onActionClicked = onChooseFromGallery,
            modifier = Modifier.testTag(GROUP_IMAGE_SOURCE_GALLERY_TAG),
        )
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

private fun createGroupImageUri(context: Context): Uri {
    val file = File.createTempFile("group_chat_image_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.providers.fileprovider",
        file,
    )
}

internal const val GROUP_IMAGE_PICKER_BUTTON_TAG = "group_image_picker:button"
internal const val GROUP_IMAGE_SOURCE_SHEET_TAG = "group_image_picker:source_sheet"
internal const val GROUP_IMAGE_SOURCE_TAKE_PHOTO_TAG = "group_image_picker:take_photo"
internal const val GROUP_IMAGE_SOURCE_GALLERY_TAG = "group_image_picker:choose_from_gallery"
