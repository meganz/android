package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.domain.entity.account.AccountDetail

/**
 * Settings ui state
 *
 * @property autoAcceptEnabled
 * @property autoAcceptChecked
 * @property multiFactorAuthChecked
 * @property multiFactorEnabled
 * @property multiFactorVisible
 * @property deleteAccountVisible
 * @property deleteEnabled
 * @property cameraUploadsEnabled
 * @property cameraUploadsOn
 * @property chatEnabled
 * @property callsEnabled
 * @property startScreen
 * @property hideRecentActivityChecked
 * @property mediaDiscoveryViewState
 * @property email
 * @property accountType
 * @property passcodeLock
 * @property subFolderMediaDiscoveryChecked
 * @property cookiePolicyLink
 * Uploads should be shown
 * @property isHiddenNodesEnabled
 * @property showHiddenItems
 * @property accountDetail
 * @property syncEnabled whether sync section should be shown
 */
data class SettingsState(
    val autoAcceptEnabled: Boolean,
    val autoAcceptChecked: Boolean,
    val multiFactorAuthChecked: Boolean,
    val multiFactorEnabled: Boolean,
    val multiFactorVisible: Boolean,
    val deleteAccountVisible: Boolean,
    val deleteEnabled: Boolean,
    val cameraUploadsEnabled: Boolean,
    val cameraUploadsOn: Boolean,
    val chatEnabled: Boolean,
    val callsEnabled: Boolean,
    val syncEnabled: Boolean,
    val startScreen: Int,
    val hideRecentActivityChecked: Boolean,
    val mediaDiscoveryViewState: Int,
    val email: String,
    val accountType: String,
    val passcodeLock: Boolean,
    val subFolderMediaDiscoveryChecked: Boolean,
    val cookiePolicyLink: String?,
    val isHiddenNodesEnabled: Boolean?,
    val showHiddenItems: Boolean,
    val accountDetail: AccountDetail?,
)
