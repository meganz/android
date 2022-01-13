package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.UserAccount
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface

interface AccountRepository {
    fun getUserAccount(): UserAccount
    fun isAccountDataStale(): Boolean
    fun requestAccount()
    fun getRootNode(): MegaNode?
    fun isMultiFactorAuthAvailable(): Boolean
    suspend fun isMultiFactorAuthEnabled(): Boolean
    fun monitorMultiFactorAuthChanges(): Flow<Boolean>
}