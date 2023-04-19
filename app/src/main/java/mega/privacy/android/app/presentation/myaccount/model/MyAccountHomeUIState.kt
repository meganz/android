package mega.privacy.android.app.presentation.myaccount.model

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import java.io.File

/**
 * My account home fragment ui state
 * @property name
 * @property email
 * @property verifiedPhoneNumber
 * @property canVerifyPhoneNumber
 * @property avatar
 * @property avatarColor
 * @property accountType
 * @property isBusinessAccount
 * @property isMasterBusinessAccount
 * @property isAchievementsEnabled
 * @property isBusinessStatusActive
 * @property visibleContacts
 * @property accountDetail
 * @property isProFlexiAccount
 * @property hasRenewableSubscription
 * @property hasExpireAbleSubscription
 * @property lastSession
 * @property usedStorage
 * @property usedStoragePercentage
 * @property usedTransfer
 * @property usedTransferPercentage
 * @property totalStorage
 * @property totalTransfer
 * @property subscriptionRenewTime
 * @property proExpirationTime
 * @property bonusStorageSms
 * @property isConnectedToNetwork
 */
data class MyAccountHomeUIState(
    val name: String? = null,
    val email: String? = null,
    val verifiedPhoneNumber: String? = null,
    val canVerifyPhoneNumber: Boolean = false,
    val avatar: File? = null,
    val avatarColor: Int? = null,
    val accountType: AccountType? = AccountType.FREE,
    val isBusinessAccount: Boolean = false,
    val isMasterBusinessAccount: Boolean = false,
    val isAchievementsEnabled: Boolean = false,
    val isBusinessStatusActive: Boolean = false,
    val visibleContacts: Int? = null,
    val accountDetail: AccountDetail? = null,
    val isProFlexiAccount: Boolean = false,
    val hasRenewableSubscription: Boolean = false,
    val hasExpireAbleSubscription: Boolean = false,
    val lastSession: Long? = null,
    val usedStorage: Long = 0,
    val usedStoragePercentage: Int = 0,
    val usedTransfer: Long = 0,
    val usedTransferPercentage: Int = 0,
    val totalStorage: Long = 0,
    val totalTransfer: Long = 0,
    val subscriptionRenewTime: Long = 0,
    val proExpirationTime: Long = 0,
    val bonusStorageSms: Long = 0,
    val isConnectedToNetwork: Boolean = true
)