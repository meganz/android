package mega.privacy.android.feature.sync.ui.views

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.navigation.StopBackupMegaPicker
import mega.privacy.android.feature.sync.navigation.SyncList
import mega.privacy.android.feature.sync.navigation.SyncNewFolder
import mega.privacy.android.feature.sync.navigation.syncNavGraph
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
import mega.privacy.android.feature.sync.ui.SyncState
import mega.privacy.android.feature.sync.ui.newfolderpair.TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.shared.sync.ui.SyncEmptyState
import timber.log.Timber

@Composable
internal fun SyncScreen(
    state: SyncState,
    navController: NavHostController,
    megaNavigator: MegaNavigator,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
    onBackPressed: () -> Unit,
    shouldNavigateToSyncList: Boolean,
    newFolderDetail: SyncNewFolder?,
    shouldOpenStopBackup: Boolean,
) {
    if (state.isNetworkConnected) {
        AndroidSyncFeatureNavigation(
            navController,
            shouldNavigateToSyncList = shouldNavigateToSyncList,
            newFolderDetail = newFolderDetail,
            shouldOpenStopBackup = shouldOpenStopBackup,
            megaNavigator = megaNavigator,
            fileTypeIconMapper = fileTypeIconMapper,
            syncPermissionsManager = syncPermissionsManager,
        )
    } else {
        SyncNoNetworkState(onBackPressed = onBackPressed)
    }
}

@Composable
internal fun AndroidSyncFeatureNavigation(
    animatedNavController: NavHostController,
    shouldNavigateToSyncList: Boolean,
    newFolderDetail: SyncNewFolder?,
    megaNavigator: MegaNavigator,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
    shouldOpenStopBackup: Boolean = false,
) {
    val context = LocalContext.current
    val startDestination = if (shouldOpenStopBackup) {
        StopBackupMegaPicker
    } else {
        newFolderDetail ?: SyncList()
    }

    val fragmentActivity = context.findFragmentActivity()
    val viewModelStoreOwner =
        fragmentActivity ?: checkNotNull(LocalViewModelStoreOwner.current)

    val syncFoldersViewModel: SyncFoldersViewModel =
        hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val syncStalledIssuesViewModel: SyncStalledIssuesViewModel =
        hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel =
        hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val syncIssueNotificationViewModel: SyncIssueNotificationViewModel =
        hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)

    NavHost(
        navController = animatedNavController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        Timber.d("shouldOpenStopBackup: $shouldOpenStopBackup")
        syncNavGraph(
            navController = animatedNavController,
            megaNavigator = megaNavigator,
            fileTypeIconMapper = fileTypeIconMapper,
            syncPermissionsManager = syncPermissionsManager,
            openUpgradeAccountPage = {
                megaNavigator.openUpgradeAccount(context)
            },
            shouldNavigateToSyncList = shouldNavigateToSyncList,
            context = context,
            syncFoldersViewModel = syncFoldersViewModel,
            syncStalledIssuesViewModel = syncStalledIssuesViewModel,
            syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
            syncIssueNotificationViewModel = syncIssueNotificationViewModel,
        )
    }
}

/**
 * A [Composable] which displays a No network connectivity state
 */
@Composable
internal fun SyncNoNetworkState(
    onBackPressed: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    MegaScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR),
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(R.string.sync_toolbar_title),
                onNavigationPressed = onBackPressed,
                elevation = 0.dp
            )
        }, content = { _ ->
            SyncEmptyState(
                iconId = iconPackR.drawable.ic_no_cloud,
                iconSize = 144.dp,
                iconDescription = "No network connectivity state",
                textId = sharedResR.string.sync_no_network_state,
                testTag = SYNC_NO_NETWORK_STATE
            )
        }
    )
}


internal const val SYNC_NO_NETWORK_STATE = "sync:no_network_state"