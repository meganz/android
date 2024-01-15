package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoHeader
import mega.privacy.android.app.presentation.fileinfo.view.MENU_ACTIONS_TO_SHOW
import mega.privacy.android.app.presentation.fileinfo.view.PreviewWithShadow
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_TOP_APPBAR
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.core.ui.controls.appbar.AppBarForCollapsibleHeader
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.layouts.ScaffoldWithCollapsibleHeader
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.shared.theme.MegaAppTheme
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Compose screen for offline file info
 */
@Composable
internal fun OfflineFileInfoScreen(
    uiState: OfflineFileInfoUiState,
    onBackPressed: () -> Unit,
    onRemoveFromOffline: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var showRemoveFromOfflineDialog by rememberSaveable { mutableStateOf(false) }

    val iconResource = when {
        uiState.thumbnail != null -> null
        uiState.isFolder -> mega.privacy.android.core.R.drawable.ic_folder_list
        else -> MimeTypeThumbnail.typeForName(uiState.title).iconResourceId
    }

    ScaffoldWithCollapsibleHeader(
        modifier = modifier,
        headerIncludingSystemBar = uiState.thumbnail?.let { previewUri ->
            {
                PreviewWithShadow(
                    previewUri = previewUri,
                )
            }
        },
        topBar = {
            AppBarForCollapsibleHeader(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = uiState.title,
                modifier = Modifier.testTag(TEST_TAG_TOP_APPBAR),
                onNavigationPressed = onBackPressed,
                maxActionsToShow = MENU_ACTIONS_TO_SHOW,
            )
        },
        header = {
            FileInfoHeader(
                title = uiState.title,
                iconResource = iconResource,
                accessPermissionDescription = null,
            )
        }
    ) {
        OfflineFileInfoContent(uiState = uiState, onRemoveFromOffline = {
            showRemoveFromOfflineDialog = true
        })
    }

    if (showRemoveFromOfflineDialog) {
        MegaAlertDialog(
            text = stringResource(id = R.string.confirmation_delete_from_save_for_offline),
            confirmButtonText = stringResource(id = R.string.general_remove),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onConfirm = onRemoveFromOffline,
            onDismiss = { showRemoveFromOfflineDialog = false }
        )
    }
}


@SuppressLint("UnrememberedMutableState")
@CombinedThemePreviews
@Composable
private fun OfflineFileInfoScreenPreview(
    @PreviewParameter(OfflineFileInfoViewStatePreviewsProvider::class) state: OfflineFileInfoUiState,
) {
    val uiState by mutableStateOf(state) // Not remembered to allow multiple states in the device, don't do that in real code, just in previews
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        OfflineFileInfoScreen(
            uiState = uiState,
            modifier = Modifier.background(color = MaterialTheme.colors.background),
            onRemoveFromOffline = { },
            onBackPressed = { },
        )
    }
}


/**
 * Provides different [OfflineFileInfoUiState] for previews
 */
internal class OfflineFileInfoViewStatePreviewsProvider :
    PreviewParameterProvider<OfflineFileInfoUiState> {
    override val values: Sequence<OfflineFileInfoUiState>
        get() = sequenceOf(
            uiStateFile,
            uiImageStateFile,
            uiStateFolder,
        )

    companion object {
        val uiStateFile = OfflineFileInfoUiState(
            title = "Notes.txt",
            folderInfo = null,
            isFolder = false,
            addedTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            totalSize = 100L,
            thumbnail = null
        )

        val uiImageStateFile = OfflineFileInfoUiState(
            title = "Photo.jpg",
            folderInfo = null,
            isFolder = false,
            addedTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            totalSize = 14500L,
            thumbnail = "/path"
        )

        val uiStateFolder = OfflineFileInfoUiState(
            title = "Favorite",
            folderInfo = OfflineFolderInfo(1, 4),
            isFolder = true,
            addedTime = Instant.now().epochSecond - 10.seconds.inWholeSeconds,
            totalSize = 5500L,
            thumbnail = null
        )
    }
}
