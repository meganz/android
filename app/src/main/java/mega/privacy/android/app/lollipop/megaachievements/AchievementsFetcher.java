package mega.privacy.android.app.lollipop.megaachievements;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.Util.calculateDateFromTimestamp;

/**
 * Fetch achievements data from server
 * Provide data to relevant UI
 */
public class AchievementsFetcher implements MegaRequestListenerInterface {
    private MegaApiJava mMegaApi;
    boolean mFetching;

    private MegaAchievementsDetails mMegaAchievements;
    private ArrayList<ReferralBonus> mReferralBonuses = new ArrayList<>();
    private DataCallback mDataCallback;
    private RequestCallback mRequestCallback;

    public synchronized void fetch() {
        if (mFetching) return;
        mFetching = true;

        if (mMegaApi == null) {
            mMegaApi = MegaApplication.getInstance().getMegaApi();
        }

        mMegaApi.getAccountAchievements(this);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish: " + request.getRequestString() + "__" + e.getErrorCode());
        mFetching = false;

        if (request.getType() == MegaRequest.TYPE_GET_ACHIEVEMENTS) {
            if (e.getErrorCode() == MegaError.API_OK) {
                mMegaAchievements = request.getMegaAchievementsDetails();
                if (mMegaAchievements != null) {
                    calculateReferralBonuses();
                    if (mDataCallback != null) {
                        mDataCallback.onAchievementsReceived();
                    }
                }
            } else if (mRequestCallback != null) {
                mRequestCallback.onRequestError();
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        mFetching = false;
    }

    // Fragments that require the achievements data should implement this interface and register as callback
    interface DataCallback {
        void onAchievementsReceived();
    }

    public void setDataCallback(DataCallback cb) {
        mDataCallback = cb;

        // Data already retrieved, notify the callback
        if (mDataCallback != null && mMegaAchievements != null) {
            mDataCallback.onAchievementsReceived();
        }
    }

    interface RequestCallback {
        void onRequestError();
    }

    public void setRequestCallback(RequestCallback cb) {
        mRequestCallback = cb;
    }

    private void calculateReferralBonuses() {
        logDebug("calculateReferralBonuses");

        long count = mMegaAchievements.getAwardsCount();

        for (int i = 0; i < count; i++) {
            int type = mMegaAchievements.getAwardClass(i);

            int awardId = mMegaAchievements.getAwardId(i);

            int rewardId = mMegaAchievements.getRewardAwardId(awardId);
            logDebug("AWARD ID: " + awardId + " REWARD id: " + rewardId);

            if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE) {

                ReferralBonus rBonus = new ReferralBonus();

                rBonus.setEmails(mMegaAchievements.getAwardEmails(i));

                long daysLeft = mMegaAchievements.getAwardExpirationTs(i);
                logDebug("Registration AwardExpirationTs: " + daysLeft);

                Calendar start = calculateDateFromTimestamp(daysLeft);
                Calendar end = Calendar.getInstance();
                Date startDate = start.getTime();
                Date endDate = end.getTime();
                long startTime = startDate.getTime();
                long endTime = endDate.getTime();
                long diffTime = startTime - endTime;
                long diffDays = diffTime / (1000 * 60 * 60 * 24);

                rBonus.setDaysLeft(diffDays);

                rBonus.setStorage(mMegaAchievements.getRewardStorageByAwardId(awardId));
                rBonus.setTransfer(mMegaAchievements.getRewardTransferByAwardId(awardId));

                mReferralBonuses.add(rBonus);
            } else {
                logDebug("MEGA_ACHIEVEMENT: " + type);
            }
        }
    }

    public MegaAchievementsDetails getAchievementsDetails() {
        return mMegaAchievements;
    }

    public ArrayList<ReferralBonus> getReferralBonuses() {
        return mReferralBonuses;
    }
}
