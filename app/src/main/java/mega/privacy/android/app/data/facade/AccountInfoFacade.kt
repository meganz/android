package mega.privacy.android.app.data.facade

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
 * Implements [AccountInfoWrapper] and saves the account details timestamps
 */
@Singleton
class AccountInfoFacade @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val accountPreferencesGateway: AccountPreferencesGateway,
) : AccountInfoWrapper {
    override suspend fun handleAccountDetail(request: MegaRequest) {
        val storage = request.numDetails and HAS_STORAGE_DETAILS != 0
        if (storage && megaApiGateway.getRootNode() != null) {
            runCatching {
                accountPreferencesGateway.setAccountDetailsTimeStamp(currentTimeInSeconds())
            }.onFailure { Timber.e(it, "Failed to save the account details timestamp") }
        }
        val megaAccountDetails = request.megaAccountDetails ?: return
        val sessions = request.numDetails and HAS_SESSIONS_DETAILS != 0
        if (sessions) {
            megaAccountDetails.getSession(0) ?: return
            Timber.d("getMegaAccountSESSION not Null")
            runCatching {
                accountPreferencesGateway.setExtendedAccountDetailsTimeStamp(currentTimeInSeconds())
            }.onFailure { Timber.e(it, "Failed to save the extended account details timestamp") }
        }
    }

    private fun currentTimeInSeconds() = System.currentTimeMillis() / 1000

    private companion object {
        const val HAS_STORAGE_DETAILS = 0x01
        const val HAS_SESSIONS_DETAILS = 0x020
    }
}
