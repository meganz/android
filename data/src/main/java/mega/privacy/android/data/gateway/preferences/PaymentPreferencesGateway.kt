package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Gateway for payment related preferences persistence.
 *
 * Preferences are scoped per account, so every entry point takes the handle of the logged in user.
 */
interface PaymentPreferencesGateway {

    /**
     * Monitor the offer campaigns the given user has dismissed the subscription offer banner for.
     *
     * @param userHandle the handle of the logged in user
     */
    fun monitorDismissedSubscriptionOfferCampaigns(userHandle: Long): Flow<Set<Long>>

    /**
     * Add an offer campaign to the ones the given user has dismissed the subscription offer banner
     * for.
     *
     * @param userHandle the handle of the logged in user
     * @param campaignId the campaign whose offers should stay hidden for this user
     */
    suspend fun addDismissedSubscriptionOfferCampaign(userHandle: Long, campaignId: Long)

    /**
     * Monitor the offer campaigns the given user has dismissed the subscription offer banner on the
     * Menu screen for.
     *
     * Tracked separately from [monitorDismissedSubscriptionOfferCampaigns] so dismissing the banner
     * on one surface leaves it visible on the other.
     *
     * @param userHandle the handle of the logged in user
     */
    fun monitorDismissedSubscriptionOfferMenuCampaigns(userHandle: Long): Flow<Set<Long>>

    /**
     * Add an offer campaign to the ones the given user has dismissed the subscription offer banner
     * on the Menu screen for.
     *
     * @param userHandle the handle of the logged in user
     * @param campaignId the campaign whose offers should stay hidden for this user
     */
    suspend fun addDismissedSubscriptionOfferMenuCampaign(userHandle: Long, campaignId: Long)

    /**
     * Get when the subscription offer screen was last shown to the given user.
     *
     * @param userHandle the handle of the logged in user
     * @return the time in milliseconds, or null when the screen has never been shown
     */
    suspend fun getSubscriptionOfferLastShownTime(userHandle: Long): Long?

    /**
     * Set when the subscription offer screen was last shown to the given user.
     *
     * @param userHandle the handle of the logged in user
     * @param timeInMillis the time the screen was shown
     */
    suspend fun setSubscriptionOfferLastShownTime(userHandle: Long, timeInMillis: Long)
}
