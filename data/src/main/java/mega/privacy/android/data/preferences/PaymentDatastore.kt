package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.preferences.PaymentPreferencesGateway
import mega.privacy.android.data.qualifier.PaymentPreference
import javax.inject.Inject

internal const val paymentPreferenceFileName = "PAYMENT_PREFERENCES"

/**
 * Datastore holding payment related preferences.
 */
internal class PaymentDatastore @Inject constructor(
    @PaymentPreference private val paymentPreferenceDataStore: DataStore<Preferences>,
) : PaymentPreferencesGateway {

    override fun monitorDismissedSubscriptionOfferCampaigns(userHandle: Long): Flow<Set<Long>> =
        paymentPreferenceDataStore.data.map {
            it[dismissedSubscriptionOfferCampaignsKey(userHandle)].toCampaignIds()
        }

    override suspend fun addDismissedSubscriptionOfferCampaign(userHandle: Long, campaignId: Long) {
        paymentPreferenceDataStore.edit {
            val key = dismissedSubscriptionOfferCampaignsKey(userHandle)
            it[key] = it[key].orEmpty() + campaignId.toString()
        }
    }

    override fun monitorDismissedSubscriptionOfferMenuCampaigns(userHandle: Long): Flow<Set<Long>> =
        paymentPreferenceDataStore.data.map {
            it[dismissedSubscriptionOfferMenuCampaignsKey(userHandle)].toCampaignIds()
        }

    override suspend fun addDismissedSubscriptionOfferMenuCampaign(
        userHandle: Long,
        campaignId: Long,
    ) {
        paymentPreferenceDataStore.edit {
            val key = dismissedSubscriptionOfferMenuCampaignsKey(userHandle)
            it[key] = it[key].orEmpty() + campaignId.toString()
        }
    }

    override suspend fun getSubscriptionOfferLastShownTime(userHandle: Long): Long? =
        paymentPreferenceDataStore.data.map {
            it[subscriptionOfferLastShownTimeKey(userHandle)]
        }.first()

    override suspend fun setSubscriptionOfferLastShownTime(userHandle: Long, timeInMillis: Long) {
        paymentPreferenceDataStore.edit {
            it[subscriptionOfferLastShownTimeKey(userHandle)] = timeInMillis
        }
    }

    private fun dismissedSubscriptionOfferCampaignsKey(userHandle: Long) =
        stringSetPreferencesKey("${userHandle}_$DISMISSED_SUBSCRIPTION_OFFER_CAMPAIGNS")

    private fun dismissedSubscriptionOfferMenuCampaignsKey(userHandle: Long) =
        stringSetPreferencesKey("${userHandle}_$DISMISSED_SUBSCRIPTION_OFFER_MENU_CAMPAIGNS")

    private fun subscriptionOfferLastShownTimeKey(userHandle: Long) =
        longPreferencesKey("${userHandle}_$SUBSCRIPTION_OFFER_LAST_SHOWN_TIME")

    private fun Set<String>?.toCampaignIds(): Set<Long> =
        orEmpty().mapNotNullTo(mutableSetOf()) { it.toLongOrNull() }

    companion object {
        private const val DISMISSED_SUBSCRIPTION_OFFER_CAMPAIGNS =
            "DISMISSED_SUBSCRIPTION_OFFER_CAMPAIGNS"
        private const val DISMISSED_SUBSCRIPTION_OFFER_MENU_CAMPAIGNS =
            "DISMISSED_SUBSCRIPTION_OFFER_MENU_CAMPAIGNS"
        private const val SUBSCRIPTION_OFFER_LAST_SHOWN_TIME = "SUBSCRIPTION_OFFER_LAST_SHOWN_TIME"
    }
}
