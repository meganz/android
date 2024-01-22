package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionHeader
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.theme.MegaAppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatAttachFileBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    onAttachFiles: (List<Uri>) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cloudDriveLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            // Manage Cloud files here
            coroutineScope.launch { sheetState.hide() }
        }

    val localLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val attachedFiles =
                    result.data?.clipData?.takeIf { it.itemCount > 0 }?.let { clipData ->
                        buildList {
                            for (i in 0 until clipData.itemCount) {
                                add(clipData.getItemAt(i).uri)
                            }
                        }
                    } ?: listOfNotNull(result.data?.data).takeIf { it.isNotEmpty() }
                attachedFiles?.let(onAttachFiles)

            }
            coroutineScope.launch { sheetState.hide() }
        }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        MenuActionHeader(
            text = stringResource(id = R.string.context_send),
            modifier = Modifier.testTag(TEST_TAG_SEND_HEADER)
        )
        MenuActionListTile(
            text = stringResource(id = R.string.attachment_upload_panel_from_cloud),
            icon = painterResource(id = R.drawable.ic_pick_cloud_drive),
            addSeparator = false,
            onActionClicked = { openCloudDrivePicker(context, cloudDriveLauncher) },
            modifier = Modifier.testTag(TEST_TAG_SEND_FROM_CLOUD)
        )
        MenuActionListTile(
            text = stringResource(id = R.string.upload_files),
            icon = painterResource(id = R.drawable.ic_upload_file),
            addSeparator = false,
            onActionClicked = { openFilePicker(localLauncher) },
            modifier = Modifier.testTag(TEST_TAG_SEND_FROM_LOCAL)
        )
    }
}

private fun openCloudDrivePicker(
    context: Context,
    cloudDriveLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, FileExplorerActivity::class.java).also {
        it.action = FileExplorerActivity.ACTION_MULTISELECT_FILE
        cloudDriveLauncher.launch(it)
    }
}

private fun openFilePicker(
    localLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent().also {
        it.action = Intent.ACTION_GET_CONTENT
        it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        it.type = "*/*"
        localLauncher.launch(it)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun ChatAttachFileBottomSheetPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatAttachFileBottomSheet(
            sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
        )
    }
}

internal const val TEST_TAG_SEND_HEADER = "chat_view:attach_panel:send_files"
internal const val TEST_TAG_SEND_FROM_CLOUD = "chat_view:attach_panel:send_files:from_cloud"
internal const val TEST_TAG_SEND_FROM_LOCAL = "chat_view:attach_panel:send_files:from_local"