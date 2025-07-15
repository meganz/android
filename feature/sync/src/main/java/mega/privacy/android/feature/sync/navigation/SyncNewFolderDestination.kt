package mega.privacy.android.feature.sync.navigation

import android.content.Intent
import android.os.Parcelable
import android.provider.DocumentsContract
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import timber.log.Timber

/**
 * Route to the add new sync screen
 */
@Parcelize
@Serializable
data class SyncNewFolder(
    val syncType: SyncType = SyncType.TYPE_TWOWAY,
    val isFromManagerActivity: Boolean = false,
    val remoteFolderHandle: Long? = null,
    val remoteFolderName: String? = null,
) : Parcelable

internal fun NavGraphBuilder.syncNewFolderDestination(
    syncPermissionsManager: SyncPermissionsManager,
    navController: NavController,
    shouldNavigateToSyncList: Boolean,
    openUpgradeAccountPage: () -> Unit,
    popToSyncListView: () -> Unit,
) {
    composable<SyncNewFolder>(
        deepLinks = listOf(
            navDeepLink<SyncNewFolder>(
                basePath = "https://mega.nz/${getSyncRoute()}/SyncNewFolder",
            ) {
                action = Intent.ACTION_VIEW
            }),
    ) { navBackStackEntry ->
        val routeArg = navBackStackEntry.toRoute<SyncNewFolder>()

        val context = LocalContext.current
        val syncType = routeArg.syncType
        val isFromManagerActivity = routeArg.isFromManagerActivity
        val remoteFolderHandle = routeArg.remoteFolderHandle
        val remoteFolderName = routeArg.remoteFolderName

        val viewModel =
            hiltViewModel<SyncNewFolderViewModel, SyncNewFolderViewModel.SyncNewFolderViewModelFactory> { factory ->
                factory.create(
                    syncType = syncType,
                    remoteFolderHandle = remoteFolderHandle,
                    remoteFolderName = remoteFolderName
                )
            }

        val launcher = launchFolderPicker(
            onFolderSelected = { uri ->
                runCatching {
                    val documentFile = if (DocumentsContract.isTreeUri(uri)) {
                        DocumentFile.fromTreeUri(context, uri)
                    } else {
                        DocumentFile.fromFile(uri.toFile())
                    }
                    documentFile?.let {
                        viewModel.handleAction(
                            SyncNewFolderAction.LocalFolderSelected(
                                documentFile
                            )
                        )
                    }
                }.onFailure {
                    Timber.Forest.e(it)
                }
            },
        )

        SyncNewFolderScreenRoute(
            viewModel = viewModel,
            syncPermissionsManager = syncPermissionsManager,
            openSelectMegaFolderScreen = {
                navController.navigate(SyncMegaPicker)
            },
            openNextScreen = {
                if (shouldNavigateToSyncList) {
                    popToSyncListView()
                } else {
                    if (!navController.popBackStack()) {
                        context.findFragmentActivity()?.finish()
                    }
                }
            },
            openUpgradeAccount = {
                openUpgradeAccountPage()
            },
            onBackClicked = {
                if (shouldNavigateToSyncList && isFromManagerActivity.not()) {
                    popToSyncListView()
                } else {
                    if (!navController.popBackStack()) {
                        context.findFragmentActivity()?.finish()
                    }
                }
            },
            onSelectFolder = {
                launcher.launch(null)
            },
        )
    }
}