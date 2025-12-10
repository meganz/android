package mega.privacy.android.feature.myaccount.presentation.model

import mega.privacy.android.domain.entity.StorageState
import java.io.File

/**
 * UI state for MyAccount widget
 *
 * @property name User's full name
 * @property accountTypeNameResource String resource ID for account type (e.g., "Pro Lite")
 * @property avatarFile User's avatar image file
 * @property avatarColor Color for avatar background if no image
 * @property usedStorage Used storage in bytes
 * @property totalStorage Total storage in bytes
 * @property usedStoragePercentage Storage usage percentage (0-100)
 * @property storageState Current storage state (e.g., Green, Orange, Red)
 * @property storageQuotaLevel Quota level determining progress bar color
 * @property isLoading Whether data is currently loading
 */
data class MyAccountWidgetUiState(
    val name: String? = null,
    val accountTypeNameResource: Int = 0,
    val avatarFile: File? = null,
    val avatarColor: Int? = null,
    val usedStorage: Long = 0,
    val totalStorage: Long = 0,
    val usedStoragePercentage: Int = 0,
    val storageState: StorageState = StorageState.Unknown,
    val storageQuotaLevel: QuotaLevel = QuotaLevel.Success,
    val isLoading: Boolean = true,
)
