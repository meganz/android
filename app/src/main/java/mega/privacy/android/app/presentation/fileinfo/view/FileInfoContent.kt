package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.fileinfo.model.ContactPermission
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.fileinfo.view.sharedinfo.SharedInfoView
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.shares.AccessPermission
import java.time.Instant.now
import kotlin.time.Duration.Companion.days

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
    onSharedWithHeaderClick: () -> Unit,
    onSharedWithContactClick: (ContactPermission) -> Unit,
    onSharedWithContactLongClick: (ContactPermission) -> Unit,
    onSharedWithContactMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreSharedWithContactsClick: () -> Unit,
    onPublicLinkCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier = modifier) {
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
        }

        //available offline
        if (isAvailableOfflineAvailable) {
            AvailableOfflineView(
                enabled = isAvailableOfflineEnabled,
                available = isAvailableOffline,
                onCheckChanged = availableOfflineChanged,
                modifier = paddingHorizontal,
            )
        }

        //file versions
        if (isFile) {
            FileVersionsView(
                versions = historyVersions,
                onClick = onVersionsClick,
                modifier = paddingEnd,
            )
        }

        //shared info (outgoing share)
        if (outShares.isNotEmpty()) {
            SharedInfoView(
                contacts = emptyList(),
                expanded = isShareContactExpanded,
                onHeaderClick = onSharedWithHeaderClick,
                onContactClick = onSharedWithContactClick,
                onContactLongClick = onSharedWithContactLongClick,
                onMoreOptionsClick = onSharedWithContactMoreOptionsClick,
                onShowMoreContactsClick = onShowMoreSharedWithContactsClick,
            )
        }

        //file size layout
        NodeSizeView(
            forFolder = !isFile,
            sizeString = Util.getSizeString(sizeInBytes),
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
                previousVersionsSizeInBytes = folderTreeInfo?.sizeOfPreviousVersionsInBytes ?: 0,
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
            ShareLinkView(
                link = publicLink,
                date = publicLinkCreationTime ?: 0,
                onCopyLinkClick = onPublicLinkCopyClick,
                modifier = paddingHorizontal,
            )
        }
    }
}

/**
 * Preview for [FileInfoContent] for a file
 */
@CombinedThemePreviews
@Composable
private fun FileInfoContentFilePreview(
    @PreviewParameter(FileInfoViewStateProvider::class) viewState: FileInfoViewState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileInfoContent(
            viewState = viewState,
            onLinkClick = {},
            availableOfflineChanged = {},
            onVersionsClick = {},
            onSharedWithContactClick = {},
            onSharedWithContactLongClick = {},
            onSharedWithContactMoreOptionsClick = {},
            onSharedWithHeaderClick = {},
            onShowMoreSharedWithContactsClick = {},
            onPublicLinkCopyClick = {},
            onLocationClick = {}
        )
    }
}

private class FileInfoViewStateProvider : PreviewParameterProvider<FileInfoViewState> {
    override val values = listOf(viewStateFile, viewStateFolder).asSequence()
}

private val viewStateFile = FileInfoViewState(
    title = "Node",
    isFile = true,
    oneOffViewEvent = null,
    jobInProgressState = null,
    historyVersions = 5,
    isNodeInInbox = false,
    isNodeInRubbish = false,
    previewUriString = null,
    thumbnailUriString = null,
    folderTreeInfo = null,
    isShareContactExpanded = false,
    outShares = emptyList(),
    nodeLocationInfo = LocationInfo("Cloud drive"),
    isAvailableOffline = false,
    isAvailableOfflineEnabled = true,
    isAvailableOfflineAvailable = true,
    inShareOwnerContactItem = contactItemForPreviews,
    accessPermission = AccessPermission.FULL,
    outShareContactShowOptions = null,
    outShareContactsSelected = emptyList(),
    iconResource = R.drawable.ic_text_thumbnail,
    sizeInBytes = 1024,
    isExported = true,
    isTakenDown = false,
    publicLink = "https://mega.co.nz/whatever",
    publicLinkCreationTime = now().epochSecond - 10.days.inWholeSeconds,
    showLink = true,
    creationTime = now().epochSecond - 10.days.inWholeSeconds,
    modificationTime = now().epochSecond,
    hasPreview = false,
)

private val viewStateFolder = FileInfoViewState(
    title = "Node",
    isFile = false,
    oneOffViewEvent = null,
    jobInProgressState = null,
    historyVersions = 5,
    isNodeInInbox = false,
    isNodeInRubbish = false,
    previewUriString = null,
    thumbnailUriString = null,
    folderTreeInfo = FolderTreeInfo(5, 6, 1024 * 15, 23, 1024 * 2),
    isShareContactExpanded = false,
    outShares = emptyList(),
    nodeLocationInfo = LocationInfo("Cloud drive"),
    isAvailableOffline = false,
    isAvailableOfflineEnabled = true,
    isAvailableOfflineAvailable = true,
    inShareOwnerContactItem = contactItemForPreviews,
    accessPermission = AccessPermission.FULL,
    outShareContactShowOptions = null,
    outShareContactsSelected = emptyList(),
    iconResource = R.drawable.ic_folder_incoming,
    sizeInBytes = 1024,
    isExported = true,
    isTakenDown = false,
    publicLink = "https://mega.co.nz/whatever",
    publicLinkCreationTime = now().epochSecond - 10.days.inWholeSeconds,
    showLink = true,
    creationTime = now().epochSecond - 10.days.inWholeSeconds,
    modificationTime = now().epochSecond,
    hasPreview = false,
)