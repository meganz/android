package mega.privacy.android.app.data.facade

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AccountPreferencesGateway
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account info facade
 *
 * Implements [AccountInfoWrapper] and provides a facade over [MyAccountInfo]
 *
 * @property myAccountInfo
 */
@Singleton
class AccountInfoFacade @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val accountPreferencesGateway: AccountPreferencesGateway,
) : AccountInfoWrapper {
    override suspend fun handleAccountDetail(request: MegaRequest) {
        val storage = request.numDetails and MyAccountInfo.HAS_STORAGE_DETAILS != 0
        if (storage && megaApiGateway.getRootNode() != null) {
            runCatching {
                accountPreferencesGateway.setAccountDetailsTimeStamp(currentTimeInSeconds())
            }.onFailure { Timber.e(it, "Failed to save the account details timestamp") }
        }
        val megaAccountDetails = request.megaAccountDetails ?: return
        // backward compatible, it will replace by new AccountDetail domain entity
        myAccountInfo.setAccountDetails(megaAccountDetails, request.numDetails, context)
        val sessions =
            request.numDetails and MyAccountInfo.HAS_SESSIONS_DETAILS != 0
        if (sessions) {
            megaAccountDetails.getSession(0) ?: return
            Timber.d("getMegaAccountSESSION not Null")
            runCatching {
                accountPreferencesGateway.setExtendedAccountDetailsTimeStamp(currentTimeInSeconds())
            }.onFailure { Timber.e(it, "Failed to save the extended account details timestamp") }
        }
    }

    override suspend fun resetAccountInfo() = myAccountInfo.resetDefaults()

    private fun currentTimeInSeconds() = System.currentTimeMillis() / 1000
}
