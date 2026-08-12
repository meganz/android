package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption

class SettingsSyncScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SettingSyncScreenDefault() {
        AndroidThemeForPreviews {
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
