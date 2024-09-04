package mega.privacy.android.feature.sync.ui.settings

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.model.SyncOption

internal data class SettingsSyncUiState(
    val syncOption: SyncOption = SyncOption.WI_FI_OR_MOBILE_DATA,
    val syncDebrisSizeInBytes: Long? = null,
    val showSyncFrequency: Boolean = false,
    val syncFrequency: SyncFrequency = SyncFrequency.EVERY_15_MINUTES,
    @StringRes val snackbarMessage: Int? = null
)