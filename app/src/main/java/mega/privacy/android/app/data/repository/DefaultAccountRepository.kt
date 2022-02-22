package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

class DefaultAccountRepository @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val sdk: MegaApiAndroid,
    @ApplicationContext private val context: Context,
) : AccountRepository {
    override fun getUserAccount(): UserAccount {
        return UserAccount(sdk.myEmail, sdk.isBusinessAccount, sdk.isMasterBusinessAccount, myAccountInfo.accountType)
    }

    override fun hasAccountBeenFetched(): Boolean {
        LogUtil.logDebug("Check the last call to getAccountDetails")
        return DBUtil.callToAccountDetails() || myAccountInfo.usedFormatted.isBlank()
    }

    override fun requestAccount() {
        (context as MegaApplication).askForAccountDetails()
    }

    override fun getRootNode(): MegaNode? {
        return sdk.rootNode
    }

    override fun isMultiFactorAuthAvailable(): Boolean {
        return sdk.multiFactorAuthAvailable()
    }

    override fun fetchMultiFactorAuthConfiguration(listenerInterface: MegaRequestListenerInterface) {
        sdk.multiFactorAuthCheck(sdk.myEmail, listenerInterface)
    }
}