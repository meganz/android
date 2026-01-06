package mega.privacy.android.feature.sync.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import javax.inject.Inject

class SyncFeatureDestination @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val syncPermissionsManager: SyncPermissionsManager,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
) : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            syncScreens(
                navigationHandler = navigationHandler,
                fileTypeIconMapper = fileTypeIconMapper,
                syncPermissionsManager = syncPermissionsManager,
                monitorThemeModeUseCase = monitorThemeModeUseCase,
                openUpgradeAccountPage = {
                    navigationHandler.navigate(UpgradeAccountNavKey())
                }
            )
        }
}
