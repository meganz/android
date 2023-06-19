package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.entity.contacts.ContactItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Mapper for  [AwardedAchievementInvite] to [ReferralBonusAchievements]
 * [ReferralBonusAchievements]
 */
class ReferralBonusAchievementsMapper @Inject constructor(
    private val numberOfDaysMapper: NumberOfDaysMapper
) {
    /**
     * Invoke
     * @param awardedAchievementInvite
     * @param contact
     */
    operator fun invoke(
        awardedAchievementInvite: AwardedAchievementInvite,
        contact: ContactItem?,
    ): ReferralBonusAchievements {
        return ReferralBonusAchievements(
            contact = contact,
            expirationInDays = numberOfDaysMapper(awardedAchievementInvite.expirationTimestampInSeconds.toMillis()),
            awardId = awardedAchievementInvite.awardId,
            expirationTimestampInSeconds = awardedAchievementInvite.expirationTimestampInSeconds,
            rewardedStorageInBytes = awardedAchievementInvite.rewardedStorageInBytes,
            rewardedTransferInBytes = awardedAchievementInvite.rewardedTransferInBytes,
            referredEmails = awardedAchievementInvite.referredEmails
        )
    }

    private fun Long.toMillis() = TimeUnit.SECONDS.toMillis(this)
}
