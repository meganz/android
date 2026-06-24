package mega.privacy.android.feature.sync.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import javax.inject.Inject

class SyncFeatureDestination @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val syncPermissionsManager: SyncPermissionsManager,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            syncScreens(
                navigationHandler = navigationHandler,
                fileTypeIconMapper = fileTypeIconMapper,
                syncPermissionsManager = syncPermissionsManager,
                monitorThemeModeUseCase = monitorThemeModeUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                openUpgradeAccountPage = {
                    navigationHandler.navigate(UpgradeAccountNavKey())
                }
            )
        }
}
