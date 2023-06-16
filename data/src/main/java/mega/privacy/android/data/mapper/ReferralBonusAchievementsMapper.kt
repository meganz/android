package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.entity.contacts.ContactData
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
     * @param contactData
     */
    operator fun invoke(
        awardedAchievementInvite: AwardedAchievementInvite,
        contactData: ContactData?,
    ): ReferralBonusAchievements {
        return ReferralBonusAchievements(
            referredAvatarUri = contactData?.avatarUri,
            referredName = contactData?.fullName,
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
