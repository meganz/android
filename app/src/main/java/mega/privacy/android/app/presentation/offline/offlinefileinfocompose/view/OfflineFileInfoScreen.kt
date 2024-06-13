package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view

import mega.privacy.android.icon.pack.R as IconPackR
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
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoHeader
import mega.privacy.android.app.presentation.fileinfo.view.MENU_ACTIONS_TO_SHOW
import mega.privacy.android.app.presentation.fileinfo.view.PreviewWithShadow
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_TOP_APPBAR
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarForCollapsibleHeader
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.ScaffoldWithCollapsibleHeader
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
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

    if (!uiState.isLoading && uiState.offlineFileInformation != null) {
        with(uiState.offlineFileInformation) {
            val iconResource = when {
                thumbnail != null -> null
                isFolder -> IconPackR.drawable.ic_folder_medium_solid
                else -> MimeTypeThumbnail.typeForName(name).iconResourceId
            }

            ScaffoldWithCollapsibleHeader(
                modifier = modifier,
                headerIncludingSystemBar = thumbnail?.let { previewUri ->
                    {
                        PreviewWithShadow(
                            previewUri = previewUri,
                        )
                    }
                },
                topBar = {
                    AppBarForCollapsibleHeader(
                        appBarType = AppBarType.BACK_NAVIGATION,
                        title = name,
                        modifier = Modifier.testTag(TEST_TAG_TOP_APPBAR),
                        onNavigationPressed = onBackPressed,
                        maxActionsToShow = MENU_ACTIONS_TO_SHOW,
                    )
                },
                header = {
                    FileInfoHeader(
                        title = name,
                        iconResource = iconResource,
                        accessPermissionDescription = null,
                    )
                },
                headerSpacerHeight = if (iconResource != null) (MAX_HEADER_HEIGHT + APP_BAR_HEIGHT).dp else MAX_HEADER_HEIGHT.dp,
            ) {
                OfflineFileInfoContent(offlineFileInformation = this) {
                    showRemoveFromOfflineDialog = true
                }
            }
        }
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
            offlineFileInformation = OfflineFileInformation(
                name = "Notes.txt",
                folderInfo = null,
                isFolder = false,
                lastModifiedTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
                totalSize = 100L,
                thumbnail = null,
                handle = "1234",
                path = ""
            ),
            isLoading = false
        )

        val uiImageStateFile = OfflineFileInfoUiState(
            offlineFileInformation = OfflineFileInformation(
                name = "Photo.jpg",
                folderInfo = null,
                isFolder = false,
                lastModifiedTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
                totalSize = 14500L,
                thumbnail = "/path",
                handle = "1234",
                path = ""
            ),
            isLoading = false
        )

        val uiStateFolder = OfflineFileInfoUiState(
            offlineFileInformation = OfflineFileInformation(
                name = "Favorite",
                folderInfo = OfflineFolderInfo(1, 4),
                isFolder = true,
                lastModifiedTime = Instant.now().epochSecond - 10.seconds.inWholeSeconds,
                totalSize = 5500L,
                thumbnail = null,
                handle = "1234",
                path = ""
            ),
            isLoading = false
        )
    }
}

private const val MAX_HEADER_HEIGHT = 96
private const val APP_BAR_HEIGHT = 56
