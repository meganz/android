package mega.privacy.android.feature.sync.ui.settings

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.ui.model.SyncOption

internal data class SettingsSyncUiState(
    val syncOption: SyncOption = SyncOption.WI_FI_OR_MOBILE_DATA,
    val syncDebrisSizeInBytes: Long? = null,
    @StringRes val snackbarMessage: Int? = null
)