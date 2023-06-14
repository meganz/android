package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.entity.contacts.ContactData
import javax.inject.Inject

/**
 * Mapper for  [AwardedAchievementInvite] to [ReferralBonusAchievements]
 * [ReferralBonusAchievements]
 */
class ReferralBonusAchievementsMapper @Inject constructor() {
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
            awardId = awardedAchievementInvite.awardId,
            expirationInDays = awardedAchievementInvite.expirationInDays,
            rewardedStorageInBytes = awardedAchievementInvite.rewardedStorageInBytes,
            rewardedTransferInBytes = awardedAchievementInvite.rewardedTransferInBytes,
            referredEmails = awardedAchievementInvite.referredEmails
        )
    }
}
