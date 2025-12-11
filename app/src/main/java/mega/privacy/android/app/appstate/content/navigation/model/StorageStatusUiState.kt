package mega.privacy.android.app.appstate.content.navigation.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.StorageState

data class StorageStatusUiState(
    val storageState: StorageState,
)
