package mega.privacy.android.data.facade

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.TelephonyGateway
import javax.inject.Inject

/**
 * Default implementation of [TelephonyGateway]
 */
internal class TelephonyFacade @Inject constructor(@ApplicationContext private val context: Context) :
    TelephonyGateway {

    override suspend fun getCurrentCountryCode(): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        return tm?.networkCountryIso
    }

    override suspend fun isRoaming(): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        return tm?.isNetworkRoaming ?: false
    }

    override suspend fun formatPhoneNumber(number: String, countryCode: String): String {
        return PhoneNumberUtils.formatNumberToE164(number, countryCode)
    }
}
