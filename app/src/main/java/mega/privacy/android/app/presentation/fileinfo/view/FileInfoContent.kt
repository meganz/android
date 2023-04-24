package mega.privacy.android.app.presentation.fileinfo.view

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.fileinfo.view.sharedinfo.SharedInfoView
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.contacts.ContactPermission

/**
 * Content for FileInfo screen, all except toolbar, bottom sheets, dialogs
 */
@Composable
internal fun FileInfoContent(
    viewState: FileInfoViewState,
    onLinkClick: (link: String) -> Unit,
    onLocationClick: () -> Unit,
    availableOfflineChanged: (checked: Boolean) -> Unit,
    onVersionsClick: () -> Unit,
    onSharedWithContactClick: (ContactPermission) -> Unit,
    onSharedWithContactLongClick: (ContactPermission) -> Unit,
    onSharedWithContactMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreSharedWithContactsClick: () -> Unit,
    onPublicLinkCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShareContactExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
    ) {
        val paddingEnd = Modifier.padding(end = 16.dp)
        val paddingHorizontal = Modifier.padding(start = 72.dp, end = 16.dp)
        with(viewState) {
            //take down alert
            var showTakeDownWarning by remember { mutableStateOf(isTakenDown) }
            if (showTakeDownWarning) {
                TakeDownWarningView(
                    isFile = isFile,
                    onLinkClick = onLinkClick,
                    onCloseClick = { showTakeDownWarning = false }
                )
            }

            //owner (incoming share)
            inShareOwnerContactItem?.let { contactItem ->
                OwnerInfoView(
                    contactItem,
                    modifier = paddingEnd
                )
                FileInfoContentDivider(16.dp)
            }

            //available offline
            if (isAvailableOfflineAvailable) {
                AvailableOfflineView(
                    enabled = isAvailableOfflineEnabled,
                    available = isAvailableOffline,
                    onCheckChanged = availableOfflineChanged,
                    modifier = paddingHorizontal,
                )
                FileInfoContentDivider()
            }

            //file versions
            if (showHistoryVersions) {
                FileVersionsView(
                    versions = historyVersions,
                    onClick = onVersionsClick,
                    modifier = paddingEnd,
                )
                FileInfoContentDivider()
            }

            //shared info (outgoing share)
            if (outShares.isNotEmpty()) {
                SharedInfoView(
                    contacts = outShares,
                    expanded = isShareContactExpanded,
                    onHeaderClick = { isShareContactExpanded = !isShareContactExpanded },
                    onContactClick = onSharedWithContactClick,
                    onContactLongClick = onSharedWithContactLongClick,
                    onMoreOptionsClick = onSharedWithContactMoreOptionsClick,
                    onShowMoreContactsClick = onShowMoreSharedWithContactsClick,
                )
                FileInfoContentDivider()
            }

            //file size layout
            Spacer(modifier = Modifier.height(8.dp))
            NodeSizeView(
                forFolder = !isFile,
                sizeString = Util.getSizeString(sizeInBytes, LocalContext.current),
                modifier = paddingHorizontal
            )

            //folder content
            folderTreeInfo?.let {
                FolderContentView(
                    numberOfFolders = it.numberOfFolders,
                    numberOfFiles = it.numberOfFiles,
                    modifier = paddingHorizontal,
                )
            }

            //folder versions
            if (showFolderHistoryVersions) {
                FolderVersionsView(
                    numberOfVersions = folderTreeInfo?.numberOfVersions ?: 0,
                    currentVersionsSizeInBytes = folderTreeInfo?.totalCurrentSizeInBytes ?: 0,
                    previousVersionsSizeInBytes = folderTreeInfo?.sizeOfPreviousVersionsInBytes
                        ?: 0,
                    modifier = paddingHorizontal,
                )
            }

            //location
            nodeLocationInfo?.location?.let {
                LocationInfoView(
                    location = it,
                    modifier = paddingHorizontal,
                    onClick = onLocationClick,
                )
            }

            //creation and modification times
            creationTime?.let {
                CreationModificationTimesView(
                    creationTimeInSeconds = it,
                    modificationTimeInSeconds = modificationTime,
                    modifier = paddingHorizontal,
                )
            }

            //link
            if (showLink && publicLink != null) {
                FileInfoContentDivider(paddingBottom = 8.dp)
                ShareLinkView(
                    link = publicLink,
                    date = publicLinkCreationTime ?: 0,
                    onCopyLinkClick = onPublicLinkCopyClick,
                    modifier = paddingHorizontal,
                )
            }
        }
    }
}

@Composable
private fun FileInfoContentDivider(
    paddingStart: Dp = paddingStartDefault.dp,
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
) {
    Divider(
        modifier = Modifier.padding(start = paddingStart, top = paddingTop, bottom = paddingBottom),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
        thickness = 1.dp
    )
}

/**
 * Preview for [FileInfoContent] for a file
 */
@SuppressLint("UnrememberedMutableState")
@CombinedThemePreviews
@Composable
private fun FileInfoContentPreview(
    @PreviewParameter(FileInfoViewStatePreviewsProvider::class) viewState: FileInfoViewState,
) {
    val scrollState = rememberScrollState()
    var state by mutableStateOf(viewState) //not remembered to allow multiple states in device, don't do that in real code, just in previews
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileInfoContent(
            viewState = state,
            onLinkClick = {},
            availableOfflineChanged = {
                state = state.copy(isAvailableOffline = !state.isAvailableOffline)
            },
            onVersionsClick = {},
            onSharedWithContactClick = {},
            onSharedWithContactLongClick = {},
            onSharedWithContactMoreOptionsClick = {},
            onShowMoreSharedWithContactsClick = {},
            onPublicLinkCopyClick = {},
            onLocationClick = {},
            modifier = Modifier.verticalScroll(scrollState)
        )
    }
}

