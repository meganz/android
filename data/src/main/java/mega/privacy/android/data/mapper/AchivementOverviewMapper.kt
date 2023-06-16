package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.achievement.Achievement
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementType.INVALID_ACHIEVEMENT
import mega.privacy.android.domain.entity.achievement.AchievementType.MEGA_ACHIEVEMENT_INVITE
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.AwardedAchievement
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaStringList

/**
 * Mapper to convert MegaAchievementsDetails to AchievementsOverview
 */
typealias AchievementsOverviewMapper = (
    @JvmSuppressWildcards MegaAchievementsDetails,
) -> @JvmSuppressWildcards AchievementsOverview

internal fun toAchievementsOverview(
    megaAchievementsDetails: MegaAchievementsDetails,
): AchievementsOverview =
    AchievementsOverview(
        allAchievements = convertAllAchievements(megaAchievementsDetails),
        awardedAchievements = convertAllAwards(megaAchievementsDetails),
        currentStorageInBytes = megaAchievementsDetails.currentStorage(),
        achievedStorageFromReferralsInBytes = megaAchievementsDetails.currentStorageReferrals(),
        achievedTransferFromReferralsInBytes = megaAchievementsDetails.currentTransferReferrals()
    )

private fun convertAllAchievements(achievementsModel: MegaAchievementsDetails): List<Achievement> =
    getAllAchievementTypes()
        .map { achievementType ->
            Achievement(
                grantStorageInBytes = achievementsModel.getClassStorage(achievementType.classValue),
                grantTransferQuotaInBytes = achievementsModel.getClassTransfer(achievementType.classValue),
                type = achievementType,
                durationInDays = achievementsModel.getClassExpire(achievementType.classValue)
            )
        }

private fun getAllAchievementTypes(): List<AchievementType> =
    AchievementType
        .values()
        .toList()
        .filter { it != INVALID_ACHIEVEMENT }

private fun convertAllAwards(megaAchievementsDetails: MegaAchievementsDetails): List<AwardedAchievement> =
    (0L until megaAchievementsDetails.awardsCount)
        .map { index ->
            val awardId = megaAchievementsDetails.getAwardId(index)

            val type = AchievementType
                .values()
                .find { it.classValue == megaAchievementsDetails.getAwardClass(index) }
                ?: INVALID_ACHIEVEMENT

            val awardedAchievement =
                AwardedAchievement(
                    awardId = awardId,
                    type = type,
                    expirationTimestampInSeconds = megaAchievementsDetails.getAwardExpirationTs(index),
                    rewardedStorageInBytes = megaAchievementsDetails.getRewardStorageByAwardId(
                        awardId
                    ),
                    rewardedTransferInBytes = megaAchievementsDetails.getRewardTransferByAwardId(
                        awardId
                    )
                )

            when (type) {
                MEGA_ACHIEVEMENT_INVITE -> {
                    val emails = getInviteEmailList(megaAchievementsDetails.getAwardEmails(index))
                    AwardedAchievementInvite(awardedAchievement, emails)
                }

                else -> {
                    awardedAchievement
                }
            }
        }
        .filter { it.type != INVALID_ACHIEVEMENT }


private fun getInviteEmailList(megaStringList: MegaStringList): List<String> {
    val emailList = mutableListOf<String>()
    for (emailIndex in 0 until megaStringList.size()) {
        emailList.add(megaStringList.get(emailIndex))
    }
    return emailList.toList()
}