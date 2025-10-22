package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.SyncListNavKey
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey
import mega.privacy.android.navigation.destination.SyncSelectStopBackupDestinationNavKey
import mega.privacy.android.navigation.megaNavigator

fun EntryProviderScope<NavKey>.syncListDestination(removeDestination: () -> Unit) {
    entry<SyncListNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openSyncs(context)
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.syncNewFolderDestination(removeDestination: () -> Unit) {
    entry<SyncNewFolderNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openNewSync(
                context = context,
                syncType = key.syncType,
                isFromManagerActivity = key.isFromManagerActivity,
                isFromCloudDrive = key.isFromCloudDrive,
                remoteFolderHandle = key.remoteFolderHandle,
                remoteFolderName = key.remoteFolderName
            )
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.syncSelectStopBackupDestinationDestination(removeDestination: () -> Unit) {
    entry<SyncSelectStopBackupDestinationNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openSelectStopBackupDestinationFromSyncsTab(
                context = context,
                folderName = key.folderName
            )
            removeDestination()
        }
    }
}
