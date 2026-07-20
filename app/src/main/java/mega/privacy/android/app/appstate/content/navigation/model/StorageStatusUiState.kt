package mega.privacy.android.app.appstate.content.navigation.model

import mega.privacy.android.domain.entity.StorageState

data class StorageStatusUiState(
    val storageState: StorageState,
    val isQuotaWarningUpsellEnabled: Boolean = false,
)
