package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

class SettingsSyncScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SettingSyncScreenDefault() {
        OriginalTheme(isDark = false) {
            SettingSyncScreen(
                uiState = SettingsSyncUiState(
                    syncConnectionType = SyncConnectionType.WiFiOrMobileData,
                    syncPowerOption = SyncPowerOption.SyncAlways,
                    pauseSyncOnBatterySaver = false,
                    syncDebrisSizeInBytes = 0L,
                ),
                syncDebrisCleared = {},
                syncConnectionTypeSelected = {},
                syncPowerOptionSelected = {},
                pauseSyncOnBatterySaverChanged = {},
                syncFrequencySelected = {},
                snackbarShown = {},
            )
        }
    }
}
