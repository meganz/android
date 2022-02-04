package mega.privacy.android.app.domain.repository

import mega.privacy.android.app.domain.entity.UserAccount
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface

interface AccountRepository {
    fun getUserAccount(): UserAccount
    fun hasAccountBeenFetched(): Boolean
    fun requestAccount()
    fun getRootNode(): MegaNode?
    fun isMultiFactorAuthAvailable(): Boolean
    fun fetchMultiFactorAuthConfiguration(listenerInterface: MegaRequestListenerInterface)
}