package mega.privacy.android.app.appstate.content.navigation.model

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.StorageQuotaWarningTrigger

/**
 * A storage quota warning that has passed the trigger rules and is ready to be shown.
 */
data class StorageQuotaWarning(
    val storageState: StorageState,
    val trigger: StorageQuotaWarningTrigger,
)
