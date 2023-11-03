package mega.privacy.android.app.presentation.fileinfo.view

import mega.privacy.android.core.R as CoreUiR
import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.palm.composestateevents.consumed
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.core.ui.controls.appbar.AppBarForCollapsibleHeader
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.controls.layouts.ScaffoldWithCollapsibleHeader
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.legacy.core.ui.controls.dialogs.LoadingDialog
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
    onVerifyContactClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionModeSelect = viewState.outShareContactsSelected.isNotEmpty()
    ScaffoldWithCollapsibleHeader(
        modifier = modifier,
        headerIncludingSystemBar = viewState.actualPreviewUriString?.takeIf { viewState.hasPreview }
            ?.let { previewUri ->
                {
                    //looks like automation tool (appium) doesn't see anything behind a scaffold,
                    // so we need to draw [FileInfoHeader] below the Scaffold. The preview needs to be drawn here to don't overlap other views.
                    PreviewWithShadow(
                        previewUri = previewUri,
                    )
                }
            },
        topBar = {
            Crossfade(
                targetState = actionModeSelect, label = "CrossfadeFileInfoTopAppBar",
            ) { actionModeSelect ->
                if (actionModeSelect) {
                    val count = viewState.outShareContactsSelected.size
                    SelectModeAppBar(
                        title = pluralStringResource(
                            R.plurals.general_selection_num_contacts, count, count
                        ),
                        onNavigationPressed = {
                            onMenuActionClick(FileInfoMenuAction.SelectionModeAction.ClearSelection)
                        },
                        actions = FileInfoMenuAction.SelectionModeAction.all(),
                        onActionPressed = { onMenuActionClick(it as FileInfoMenuAction) },
                        modifier = Modifier.fillMaxWidth(),
                        elevation = AppBarDefaults.TopAppBarElevation,
                    )
                } else {
                    AppBarForCollapsibleHeader(
                        appBarType = AppBarType.BACK_NAVIGATION,
                        title = viewState.title,
                        modifier = Modifier.testTag(TEST_TAG_TOP_APPBAR),
                        actions = viewState.actions,
                        onNavigationPressed = onBackPressed,
                        onActionPressed = { onMenuActionClick(it as FileInfoMenuAction) },
                        enabled = viewState.jobInProgressState == null,
                        maxActionsToShow = MENU_ACTIONS_TO_SHOW,
                    )
                }
            }
        },
        header = {
            FileInfoHeader(
                title = viewState.title,
                iconResource = viewState.iconResource?.takeIf { !viewState.hasPreview },
                accessPermissionDescription = viewState.accessPermission.description()
                    ?.takeIf { viewState.isIncomingSharedNode },
            )
        },
        headerBelowTopBar = actionModeSelect, //actionMode doesn't have collapsible title, the header needs to be drawn below the app bar
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                MegaSnackbar(snackbarData = data)
            }
        },
    ) {
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
            onVerifyContactClick = onVerifyContactClick,
        )
        viewState.jobInProgressState?.progressMessage?.let {
            LoadingDialog(text = stringResource(id = it))
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
            onVerifyContactClick = {},
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
            isNodeInBackups = false,
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
            isRemindersForContactVerificationEnabled = true,
            actions = listOf(FileInfoMenuAction.Move, FileInfoMenuAction.Copy)
        )

        val viewStateFile2 = FileInfoViewState(
            title = "File with long name taken down",
            isFile = true,
            oneOffViewEvent = consumed(),
            jobInProgressState = null,
            historyVersions = 0,
            isNodeInBackups = false,
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
            isRemindersForContactVerificationEnabled = true,
            actions = listOf(FileInfoMenuAction.Move, FileInfoMenuAction.Copy)
        )

        val viewStateFolder = FileInfoViewState(
            title = "Folder with a very long name",
            isFile = false,
            oneOffViewEvent = consumed(),
            jobInProgressState = null,
            historyVersions = 0,
            isNodeInBackups = false,
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
            isRemindersForContactVerificationEnabled = true,
            actions = listOf(FileInfoMenuAction.Move, FileInfoMenuAction.Copy)
        )

        val viewStateFolder2 = FileInfoViewState(
            title = "Folder",
            isFile = false,
            oneOffViewEvent = consumed(),
            jobInProgressState = null,
            historyVersions = 0,
            isNodeInBackups = false,
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
            isRemindersForContactVerificationEnabled = true,
            actions = listOf(FileInfoMenuAction.Move, FileInfoMenuAction.Copy)
        )
    }
}