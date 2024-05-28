package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import mega.privacy.android.icon.pack.R as iconPackR
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionHeader
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Bottom sheet to select where to get the file to attach (cloud drive or upload from device) and return the selection.
 */
@Composable
fun ChatAttachFileBottomSheet(
    modifier: Modifier = Modifier,
    hideSheet: () -> Unit,
    onAttachFiles: (List<Uri>) -> Unit = {},
    onAttachNodes: (List<NodeId>) -> Unit = {},
) {
    val context = LocalContext.current

    val cloudDriveLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getLongArrayExtra(Constants.NODE_HANDLES)
                    ?.map { NodeId(it) }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(onAttachNodes)
            }
            hideSheet()
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
            hideSheet()
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
            dividerType = null,
            onActionClicked = { openCloudDrivePicker(context, cloudDriveLauncher) },
            modifier = Modifier.testTag(TEST_TAG_SEND_FROM_CLOUD)
        )
        MenuActionListTile(
            text = stringResource(id = R.string.upload_files),
            icon = painterResource(id = iconPackR.drawable.ic_file_upload_medium_regular_outline),
            dividerType = null,
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChatAttachFileBottomSheet(
            hideSheet = {},
        )
    }
}

internal const val TEST_TAG_SEND_HEADER = "chat_view:attach_panel:send_files"
internal const val TEST_TAG_SEND_FROM_CLOUD = "chat_view:attach_panel:send_files:from_cloud"
internal const val TEST_TAG_SEND_FROM_LOCAL = "chat_view:attach_panel:send_files:from_local"