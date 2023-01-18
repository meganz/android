package mega.privacy.android.app.listeners

import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.main.megaachievements.ReferralBonus
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

/**
 * GetAchievementsListener
 */
@ActivityScoped
@Deprecated("You should call GetAccountAchievementsOverview instead")
class GetAchievementsListener @Inject constructor() : MegaRequestListenerInterface {

    /**
     * isFetching: Boolean
     */
    var isFetching = false

    /**
     * achievementsDetails : MegaAchievementsDetails
     */
    var achievementsDetails: MegaAchievementsDetails? = null

    /**
     * referralBonuses: Arraylist of ReferralBonus
     */
    val referralBonuses = ArrayList<ReferralBonus>()
    private var dataCallback: DataCallback? = null
    private var requestCallback: RequestCallback? = null

    /**
     * Get Account achievements data
     */
    @Synchronized
    fun fetch() {
        if (isFetching) return
        isFetching = true
        getInstance().megaApi.getAccountAchievements(this)
    }

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: %s__%d", request.requestString, e.errorCode)
        isFetching = false
        if (request.type == MegaRequest.TYPE_GET_ACHIEVEMENTS) {
            if (e.errorCode == MegaError.API_OK) {
                achievementsDetails = request.megaAchievementsDetails
                achievementsDetails?.let {
                    calculateReferralBonuses(it)
                    dataCallback?.onAchievementsReceived()
                }
            } else {
                requestCallback?.onRequestError()
            }
        }
    }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        isFetching = false
    }

    /**
     * Fragments that require the achievements data should implement this interface and register as callback
     */
    interface DataCallback {

        /**
         * onAchievementsReceived
         */
        fun onAchievementsReceived()
    }

    /**
     * setDataCallback
     * @param cb: DataCallback
     */
    fun setDataCallback(cb: DataCallback?) {
        dataCallback = cb

        // Data already retrieved, notify the callback
        if (achievementsDetails != null) {
            dataCallback?.onAchievementsReceived()
        }
    }

    /**
     * RequestCallback
     */
    interface RequestCallback {

        /**
         * onRequestError
         */
        fun onRequestError()
    }

    /**
     * setRequestCallback
     * @param cb: RequestCallback
     */
    fun setRequestCallback(cb: RequestCallback?) {
        requestCallback = cb
    }

    /**
     * Calculates referral bonus
     */
    private fun calculateReferralBonuses(achievementsDetails: MegaAchievementsDetails) {
        Timber.d("calculateReferralBonuses")
        with(achievementsDetails) {
            for (awardIndex in 0 until awardsCount) {
                Timber.d("AWARD ID: %d REWARD id: %d",
                    getAwardId(awardIndex),
                    getRewardAwardId(getAwardId(awardIndex).toLong()))

                if (getAwardClass(awardIndex) == MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE) {
                    val rBonus = ReferralBonus()
                    rBonus.emails = getAwardEmails(awardIndex)
                    Timber.d("Registration AwardExpirationTs: %s",
                        getAwardExpirationTs(awardIndex))
                    val start =
                        Util.calculateDateFromTimestamp(getAwardExpirationTs(awardIndex))
                    val end = Calendar.getInstance()
                    val startDate = start.time
                    val endDate = end.time
                    val startTime = startDate.time
                    val endTime = endDate.time
                    val diffTime = startTime - endTime
                    val diffDays = diffTime / (1000 * 60 * 60 * 24)
                    rBonus.daysLeft = diffDays
                    rBonus.storage = getRewardStorageByAwardId(getAwardId(awardIndex))
                    rBonus.transfer = getRewardTransferByAwardId(getAwardId(awardIndex))
                    referralBonuses.add(rBonus)
                } else {
                    Timber.d("MEGA_ACHIEVEMENT: %s", getAwardClass(awardIndex))
                }
            }
        }
    }
}