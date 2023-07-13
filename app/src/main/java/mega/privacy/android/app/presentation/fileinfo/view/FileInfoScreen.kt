package mega.privacy.android.app.presentation.fileinfo.view

import mega.privacy.android.core.R as CoreUiR
import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.consumed
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoJobInProgressState
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.transfers.view.TransferInProgressDialog
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.controls.dialogs.LoadingDialog
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.utils.MinimumTimeVisibility
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission
import java.time.Instant
import kotlin.time.Duration.Companion.days

/**
 * View to render the File Info Screen, including toolbar, content, etc.
 */
@Composable
internal fun FileInfoScreen(
    viewState: FileInfoViewState,
    snackBarHostState: SnackbarHostState,
    onBackPressed: () -> Unit,
    onTakeDownLinkClick: (link: String) -> Unit,
    onLocationClick: () -> Unit,
    availableOfflineChanged: (checked: Boolean) -> Unit,
    onVersionsClick: () -> Unit,
    onSharedWithContactClick: (ContactPermission) -> Unit,
    onSharedWithContactSelected: (ContactPermission) -> Unit,
    onSharedWithContactUnselected: (ContactPermission) -> Unit,
    onSharedWithContactMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreSharedWithContactsClick: () -> Unit,
    onPublicLinkCopyClick: () -> Unit,
    onMenuActionClick: (FileInfoMenuAction) -> Unit,
    onTransferCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current.density
        val statusBarHeight = Util.getStatusBarHeight().toFloat() / density
        val tintColorBase = MaterialTheme.colors.onSurface
        val actionModeSelect = viewState.outShareContactsSelected.isNotEmpty()
        val headerHeight by remember {
            derivedStateOf {
                (headerMaxHeight(statusBarHeight) - (scrollState.value / density))
                    .coerceAtLeast(headerMinHeight(statusBarHeight))
            }
        }
        val titleDisplacement by remember {
            derivedStateOf {
                ((headerHeight - headerGoneHeight(statusBarHeight)) * 0.5f).coerceAtLeast(0f)
            }
        }
        val longTitleAlpha by remember {
            derivedStateOf {
                (titleDisplacement / (appBarHeight / 2)).coerceIn(0f, 1f)
            }
        }

        val headerBackgroundAlpha by remember {
            derivedStateOf {
                ((headerHeight - headerGoneHeight(statusBarHeight))
                        / (headerStartGoneHeight(statusBarHeight) - headerGoneHeight(statusBarHeight)))
                    .coerceIn(0f, 1f)
            }
        }
        val topBarOpacityTransitionDelta by remember {
            derivedStateOf {
                1 - ((headerHeight - headerMinHeight(statusBarHeight))
                        / (headerGoneHeight(statusBarHeight) - headerMinHeight(statusBarHeight)))
                    .coerceIn(0f, 1f)
            }
        }
        val tintColor by remember(viewState.hasPreview) {
            derivedStateOf {
                if (viewState.hasPreview) {
                    lerp(tintColorBase, white, headerBackgroundAlpha)
                } else {
                    tintColorBase
                }
            }
        }
        FileInfoHeader(
            title = viewState.title,
            titleAlpha = longTitleAlpha,
            titleDisplacement = titleDisplacement.dp,
            tintColor = tintColor,
            backgroundAlpha = headerBackgroundAlpha,
            previewUri = viewState.actualPreviewUriString?.takeIf { viewState.hasPreview },
            iconResource = viewState.iconResource,
            accessPermissionDescription = viewState.accessPermission.description()
                ?.takeIf { viewState.isIncomingSharedNode },
            modifier = Modifier
                .height(headerHeight.dp),
            statusBarHeight = statusBarHeight.dp,
        )
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            backgroundColor = Color.Transparent,
            topBar = {
                Crossfade(
                    targetState = actionModeSelect,
                ) { actionModeSelect ->
                    if (actionModeSelect) {
                        FileInfoSelectActionModeTopBar(
                            count = viewState.outShareContactsSelected.size,
                            onActionClick = onMenuActionClick
                        )
                    } else {
                        FileInfoTopBar(
                            modifier = Modifier.testTag(TEST_TAG_TOP_APPBAR),
                            title = viewState.title,
                            actions = viewState.actions,
                            tintColor = tintColor,
                            titleDisplacement = titleDisplacement.dp,
                            titleAlpha = 1 - longTitleAlpha,
                            opacityTransitionDelta = topBarOpacityTransitionDelta,
                            onBackPressed = onBackPressed,
                            onActionClick = onMenuActionClick,
                            enabled = viewState.jobInProgressState == null
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState) { data ->
                    MegaSnackbar(snackbarData = data)
                }
            },
        ) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                //to set the minimum height of the colum so it's always possible to collapse the header
                val boxWithConstraintsScope = this
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                ) {
                    Spacer(Modifier.height(spacerHeight(statusBarHeight).dp)) //to give space for the header (that it's outside this column)
                    FileInfoContent(
                        viewState = viewState,
                        onTakeDownLinkClick = onTakeDownLinkClick,
                        onLocationClick = onLocationClick,
                        availableOfflineChanged = availableOfflineChanged,
                        onVersionsClick = onVersionsClick,
                        onContactClick = onSharedWithContactClick,
                        onContactSelected = onSharedWithContactSelected,
                        onContactUnselected = onSharedWithContactUnselected,
                        onContactMoreOptionsClick = onSharedWithContactMoreOptionsClick,
                        onContactsClosed = { onMenuActionClick(FileInfoMenuAction.SelectionModeAction.ClearSelection) },
                        onShowMoreContactsClick = onShowMoreSharedWithContactsClick,
                        onPublicLinkCopyClick = onPublicLinkCopyClick,
                        modifier = Modifier.heightIn(
                            min = boxWithConstraintsScope.maxHeight
                        )
                    )
                }
            }
        }

        when (viewState.jobInProgressState) {
            null, FileInfoJobInProgressState.ProcessingFiles -> {
                MinimumTimeVisibility(visible = viewState.jobInProgressState != null) {
                    TransferInProgressDialog(onCancelConfirmed = onTransferCancelled)
                }
            }

            else -> viewState.jobInProgressState.progressMessage?.let {
                LoadingDialog(text = stringResource(id = it))
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
            modifier = Modifier.background(color = MaterialTheme.colors.background),
            viewState = state,
            snackBarHostState = SnackbarHostState(),
            onBackPressed = {},
            onTakeDownLinkClick = {},
            availableOfflineChanged = {
                state = state.copy(isAvailableOffline = !state.isAvailableOffline)
            },
            onVersionsClick = {},
            onSharedWithContactClick = {},
            onSharedWithContactSelected = {
                state =
                    state.copy(outShareContactsSelected = state.outShareContactsSelected + it.contactItem.email)
            },
            onSharedWithContactUnselected = {
                state =
                    state.copy(outShareContactsSelected = state.outShareContactsSelected - it.contactItem.email)
            },
            onSharedWithContactMoreOptionsClick = {},
            onShowMoreSharedWithContactsClick = {},
            onPublicLinkCopyClick = {},
            onLocationClick = {},
            onTransferCancelled = {},
            onMenuActionClick = { action ->
                when (action) {
                    FileInfoMenuAction.SelectionModeAction.ClearSelection -> {
                        state = state.copy(outShareContactsSelected = emptyList())
                    }

                    FileInfoMenuAction.SelectionModeAction.SelectAll -> {
                        state = state.copy(
                            outShareContactsSelected = state.outSharesCoerceMax.map { it.contactItem.email })
                    }

                    else -> {}
                }
            },
        )
    }
}

internal const val appBarHeight = 56f
private fun headerMinHeight(statusBarHeight: Float) = appBarHeight + statusBarHeight
private fun headerMaxHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 96f
private fun headerStartGoneHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 76f
private fun headerGoneHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 18f
private fun spacerHeight(statusBarHeight: Float) =
    headerMaxHeight(statusBarHeight) - appBarHeight - statusBarHeight

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
            oneOffViewEvent = consumed(),
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
            contactToShowOptions = null,
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
            actions = listOf(FileInfoMenuAction.Move, FileInfoMenuAction.Copy)
        )

        val viewStateFile2 = FileInfoViewState(
            title = "File with long name taken down",
            isFile = true,
            oneOffViewEvent = consumed(),
            jobInProgressState = null,
            historyVersions = 0,
            isNodeInInbox = false,
            isNodeInRubbish = false,
            previewUriString = null,
            thumbnailUriString = null,
            folderTreeInfo = null,
            outShares = List(6) {
                ContactPermission(contactItemForPreviews(it), AccessPermission.READWRITE)
            },
            nodeLocationInfo = LocationInfo("Cloud drive"),
            isAvailableOffline = false,
            isAvailableOfflineEnabled = true,
            isAvailableOfflineAvailable = true,
            inShareOwnerContactItem = null,
            accessPermission = AccessPermission.FULL,
            contactToShowOptions = null,
            outShareContactsSelected = emptyList(),
            iconResource = R.drawable.ic_text_thumbnail,
            sizeInBytes = 1024,
            isExported = true,
            isTakenDown = true,
            publicLink = null,
            publicLinkCreationTime = null,
            showLink = false,
            creationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            modificationTime = null,
            hasPreview = false,
            actions = listOf(FileInfoMenuAction.Move, FileInfoMenuAction.Copy)
        )

        val viewStateFolder = FileInfoViewState(
            title = "Folder with a very long name",
            isFile = false,
            oneOffViewEvent = consumed(),
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
            contactToShowOptions = null,
            outShareContactsSelected = emptyList(),
            iconResource = CoreUiR.drawable.ic_folder_incoming,
            sizeInBytes = 1024,
            isExported = true,
            isTakenDown = false,
            publicLink = "https://mega.co.nz/whatever",
            publicLinkCreationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            showLink = true,
            creationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            modificationTime = Instant.now().epochSecond,
            hasPreview = false,
            actions = listOf(FileInfoMenuAction.Move, FileInfoMenuAction.Copy)
        )

        val viewStateFolder2 = FileInfoViewState(
            title = "Folder",
            isFile = false,
            oneOffViewEvent = consumed(),
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
                ContactPermission(contactItemForPreviews(it), AccessPermission.READWRITE)
            },
            inShareOwnerContactItem = contactItemForPreviews,
            accessPermission = AccessPermission.FULL,
            contactToShowOptions = null,
            outShareContactsSelected = emptyList(),
            iconResource = CoreUiR.drawable.ic_folder_incoming,
            sizeInBytes = 1024,
            isExported = true,
            isTakenDown = false,
            publicLink = "https://mega.co.nz/whatever",
            publicLinkCreationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            showLink = true,
            creationTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
            modificationTime = Instant.now().epochSecond,
            hasPreview = false,
            actions = listOf(FileInfoMenuAction.Move, FileInfoMenuAction.Copy)
        )
    }
}