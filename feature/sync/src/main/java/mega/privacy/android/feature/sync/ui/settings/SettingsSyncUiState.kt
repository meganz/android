package mega.privacy.android.feature.sync.ui.settings

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption

internal data class SettingsSyncUiState(
    val syncConnectionType: SyncConnectionType = SyncConnectionType.WiFiOrMobileData,
    val syncPowerOption: SyncPowerOption = SyncPowerOption.SyncAlways,
    val syncDebrisSizeInBytes: Long? = null,
    val showSyncFrequency: Boolean = false,
    val syncFrequency: SyncFrequency = SyncFrequency.EVERY_15_MINUTES,
    @StringRes val snackbarMessage: Int? = null
)
