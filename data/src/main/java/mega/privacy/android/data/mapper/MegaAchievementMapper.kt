package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.DefaultMegaAchievement
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaStringList

/**
 * Mapper class for mapping MegaAchievementsDetails to MegaAchievement
 */
typealias MegaAchievementMapper = (
    @JvmSuppressWildcards MegaAchievementsDetails,
    @JvmSuppressWildcards AchievementType,
    @JvmSuppressWildcards Long,
) -> @JvmSuppressWildcards MegaAchievement

internal fun toMegaAchievement(
    megaAchievementsDetails: MegaAchievementsDetails,
    achievementType: AchievementType,
    awardIndex: Long,
): MegaAchievement = with(megaAchievementsDetails) {
    DefaultMegaAchievement(
        invitedEmails = getInviteEmailList(getAwardEmails(awardIndex)),
        grantedStorage = getClassStorage(achievementType.classValue),
        grantedTransferQuota = getClassTransfer(achievementType.classValue),
        unlockedAwardsCount = awardsCount,
        achievementType = achievementType,
        awardId = getAwardId(awardIndex),
        awardExpirationTimeStamp = getAwardExpirationTs(awardIndex),
        rewardAwardId = getRewardAwardId(getAwardId(awardIndex).toLong()),
        rewardStorageByAwardId = getAwardId(awardIndex).toLong(),
        rewardTransferByAwardId = getAwardId(awardIndex).toLong(),
    )
}

private fun getInviteEmailList(megaStringList: MegaStringList): List<String> {
    val emailList = mutableListOf<String>()
    for (emailIndex in 0 until megaStringList.size()) {
        emailList.add(megaStringList.get(emailIndex))
    }
    return emailList.toList()
}




