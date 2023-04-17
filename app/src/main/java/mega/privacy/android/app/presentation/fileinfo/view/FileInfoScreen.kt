package mega.privacy.android.app.presentation.fileinfo.view

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.fileinfo.model.ContactPermission
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.shares.AccessPermission
import java.time.Instant
import kotlin.time.Duration.Companion.days

/**
 * View to render the File Info Screen, including toolbar, content, etc.
 */
@Composable
internal fun FileInfoScreen(
    viewState: FileInfoViewState,
    onBackPressed: () -> Unit,
    onLinkClick: (link: String) -> Unit,
    onLocationClick: () -> Unit,
    availableOfflineChanged: (checked: Boolean) -> Unit,
    onVersionsClick: () -> Unit,
    onSharedWithContactClick: (ContactPermission) -> Unit,
    onSharedWithContactLongClick: (ContactPermission) -> Unit,
    onSharedWithContactMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreSharedWithContactsClick: () -> Unit,
    onPublicLinkCopyClick: () -> Unit,
    onMenuActionClick: (FileInfoMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current.density
        val tintColorBase = MaterialTheme.colors.onSurface
        val headerHeight by remember {
            derivedStateOf { headerMaxHeight - (scrollState.value / density).coerceAtLeast(0f) }
        }

        val alpha by remember {
            derivedStateOf {
                ((headerHeight - headerGoneHeight) / (headerStartGoneHeight - headerGoneHeight))
                    .coerceIn(0f, 1f)
            }
        }
        val topBarOpacityTransitionDelta by remember {
            derivedStateOf { 1 - (headerHeight / headerGoneHeight).coerceIn(0f, 1f) }
        }
        val tintColor by remember {
            derivedStateOf {
                if (viewState.hasPreview) {
                    lerp(tintColorBase, white, alpha)
                } else {
                    tintColorBase
                }
            }
        }
        FileInfoHeader(
            viewState = viewState,
            modifier = Modifier
                .alpha(alpha)
                .height(headerHeight.dp)
        )
        Scaffold(
            backgroundColor = Color.Transparent,
            topBar = {
                FileInfoTopBar(
                    viewState = viewState,
                    tintColor = tintColor,
                    opacityTransitionDelta = topBarOpacityTransitionDelta,
                    onBackPressed = onBackPressed,
                    onActionClick = onMenuActionClick,
                )
            }) { innerPadding ->
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                //to set the minimum height of the colum so it's always possible to collapse the header
                val boxWithConstraintsScope = this
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                ) {
                    Spacer(Modifier.height(spacerHeight.dp))
                    FileInfoContent(
                        viewState = viewState,
                        onLinkClick = onLinkClick,
                        onLocationClick = onLocationClick,
                        availableOfflineChanged = availableOfflineChanged,
                        onVersionsClick = onVersionsClick,
                        onSharedWithContactClick = onSharedWithContactClick,
                        onSharedWithContactLongClick = onSharedWithContactLongClick,
                        onSharedWithContactMoreOptionsClick = onSharedWithContactMoreOptionsClick,
                        onShowMoreSharedWithContactsClick = onShowMoreSharedWithContactsClick,
                        onPublicLinkCopyClick = onPublicLinkCopyClick,
                        modifier = Modifier.heightIn(
                            min = boxWithConstraintsScope.maxHeight
                        )
                    )
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@CombinedThemePreviews
@Composable
private fun FileInfoScreenPreview(
    @PreviewParameter(FileInfoViewStatePreviewsProvider::class) viewState: FileInfoViewState,
) {
    var state by mutableStateOf(viewState) //not remembered to allow multiple states in device, don't do that in real code, just in previews
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileInfoScreen(
            viewState = state,
            onBackPressed = {},
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
            onMenuActionClick = {},
        )
    }
}


private const val headerMaxHeight = 160f
private const val headerStartGoneHeight = 110f
private const val headerGoneHeight = 66f
private const val appBarHeight = 44f
private const val spacerHeight = headerMaxHeight - appBarHeight

/**
 * Provides different [FileInfoViewState] for previews
 */
internal class FileInfoViewStatePreviewsProvider : PreviewParameterProvider<FileInfoViewState> {
    override val values
        get() =
            listOf(
                viewStateFile,
                viewStateFile2,
                viewStateFolder,
                viewStateFolder2,
            ).asSequence()

    companion object {
        val viewStateFile = FileInfoViewState(
            title = "File",
            isFile = true,
            oneOffViewEvent = null,
            jobInProgressState = null,
            historyVersions = 5,
            isNodeInInbox = false,
            isNodeInRubbish = false,
            previewUriString = null,
            thumbnailUriString = "something",
            folderTreeInfo = null,
            nodeLocationInfo = LocationInfo("Cloud drive"),
            isAvailableOffline = false,
            isAvailableOfflineEnabled = true,
            isAvailableOfflineAvailable = true,
            inShareOwnerContactItem = null,
            accessPermission = AccessPermission.FULL,
            outShareContactShowOptions = null,
            outShareContactsSelected = emptyList(),
            iconResource = R.drawable.ic_text_thumbnail,
            sizeInBytes = 1024,
            isExported = true,
            isTakenDown = false,
            publicLink = "https://mega.co.nz/whatever",
            publicLinkCreationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            showLink = true,
            creationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            modificationTime = Instant.now().epochSecond,
            hasPreview = true,
        )

        val viewStateFile2 = FileInfoViewState(
            title = "File with very long name",
            isFile = true,
            oneOffViewEvent = null,
            jobInProgressState = null,
            historyVersions = 0,
            isNodeInInbox = false,
            isNodeInRubbish = false,
            previewUriString = null,
            thumbnailUriString = null,
            folderTreeInfo = null,
            outShares = List(6) {
                ContactPermission(contactItemForPreviews, AccessPermission.READWRITE)
            },
            nodeLocationInfo = LocationInfo("Cloud drive"),
            isAvailableOffline = false,
            isAvailableOfflineEnabled = true,
            isAvailableOfflineAvailable = true,
            inShareOwnerContactItem = null,
            accessPermission = AccessPermission.FULL,
            outShareContactShowOptions = null,
            outShareContactsSelected = emptyList(),
            iconResource = R.drawable.ic_text_thumbnail,
            sizeInBytes = 1024,
            isExported = true,
            isTakenDown = false,
            publicLink = null,
            publicLinkCreationTime = null,
            showLink = false,
            creationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            modificationTime = null,
            hasPreview = false,
        )

        val viewStateFolder = FileInfoViewState(
            title = "Folder with a very long name",
            isFile = false,
            oneOffViewEvent = null,
            jobInProgressState = null,
            historyVersions = 0,
            isNodeInInbox = false,
            isNodeInRubbish = false,
            previewUriString = null,
            thumbnailUriString = null,
            folderTreeInfo = FolderTreeInfo(5, 6, 1024 * 15, 23, 1024 * 2),
            nodeLocationInfo = LocationInfo("Cloud drive"),
            isAvailableOffline = false,
            isAvailableOfflineEnabled = true,
            isAvailableOfflineAvailable = true,
            inShareOwnerContactItem = null,
            accessPermission = AccessPermission.FULL,
            outShareContactShowOptions = null,
            outShareContactsSelected = emptyList(),
            iconResource = R.drawable.ic_folder_incoming,
            sizeInBytes = 1024,
            isExported = true,
            isTakenDown = false,
            publicLink = "https://mega.co.nz/whatever",
            publicLinkCreationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            showLink = true,
            creationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            modificationTime = Instant.now().epochSecond,
            hasPreview = false,
        )

        val viewStateFolder2 = FileInfoViewState(
            title = "Folder",
            isFile = false,
            oneOffViewEvent = null,
            jobInProgressState = null,
            historyVersions = 0,
            isNodeInInbox = false,
            isNodeInRubbish = false,
            previewUriString = null,
            thumbnailUriString = null,
            folderTreeInfo = FolderTreeInfo(5, 6, 1024 * 15, 23, 1024 * 2),
            nodeLocationInfo = LocationInfo("Cloud drive"),
            isAvailableOffline = false,
            isAvailableOfflineEnabled = true,
            isAvailableOfflineAvailable = false,
            outShares = List(6) {
                ContactPermission(contactItemForPreviews, AccessPermission.READWRITE)
            },
            inShareOwnerContactItem = contactItemForPreviews,
            accessPermission = AccessPermission.FULL,
            outShareContactShowOptions = null,
            outShareContactsSelected = emptyList(),
            iconResource = R.drawable.ic_folder_incoming,
            sizeInBytes = 1024,
            isExported = true,
            isTakenDown = false,
            publicLink = "https://mega.co.nz/whatever",
            publicLinkCreationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            showLink = true,
            creationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            modificationTime = Instant.now().epochSecond,
            hasPreview = false,
        )
    }
}