package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.achievement.AchievementType.MEGA_ACHIEVEMENT_WELCOME
import mega.privacy.android.domain.entity.achievement.AchievementType.MEGA_ACHIEVEMENT_INVITE
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaStringList
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

class AchievementsOverviewMapperTest {

    private val emailMegaStringList = mock<MegaStringList> {
        on { get(0) }.thenReturn("john@yopmail.com")
        on { get(1) }.thenReturn("doe@yopmail.com")
        on { size() }.thenReturn(2)
    }

    private val rewardedStorage = 999L
    private val rewardedTransfer = 888L
    private val storageFromReferrals = 3232L
    private val transferFromReferrals = 2313233L
    private val expirationTimestamp = 100000L
    private val awardId = 1
    private val index = 0L

    private val megaAchievementsDetails = mock<MegaAchievementsDetails> {
        on { getAwardClass(index) }.thenReturn(MEGA_ACHIEVEMENT_WELCOME.classValue)
        on { getAwardExpirationTs(index) }.thenReturn(expirationTimestamp)
        on { getClassStorage(MEGA_ACHIEVEMENT_WELCOME.classValue) }.thenReturn(
            rewardedStorage)
        on { getClassTransfer(MEGA_ACHIEVEMENT_WELCOME.classValue) }.thenReturn(
            rewardedTransfer)
        on { getClassExpire(MEGA_ACHIEVEMENT_WELCOME.classValue) }.thenReturn(
            expirationTimestamp.toInt())
        on { currentTransferReferrals() }.thenReturn(transferFromReferrals)
        on { currentStorageReferrals() }.thenReturn(storageFromReferrals)
        on { awardsCount }.thenReturn(1)
        on { getAwardId(index) }.thenReturn(awardId)
        on { getRewardAwardId(index) }.thenReturn(
            awardId)
        on { getRewardStorageByAwardId(awardId) }.thenReturn(rewardedStorage)
        on { getRewardTransferByAwardId(awardId) }.thenReturn(rewardedTransfer)
    }

    @Test
    fun `convert to achievement overview EXPECT awards count is the same`() {
        val achievementOverview =
            toAchievementsOverview(megaAchievementsDetails = megaAchievementsDetails)

        val actual = achievementOverview.awardedAchievements.size.toLong()

        assertEquals(megaAchievementsDetails.awardsCount, actual)
    }

    @Test
    fun `convert to achievement overview EXPECT reward storage is the same`() {
        val achievementOverview =
            toAchievementsOverview(megaAchievementsDetails = megaAchievementsDetails)

        val expected = rewardedStorage
        val actual =
            achievementOverview.awardedAchievements.first { it.awardId == awardId }.rewardedStorageInBytes

        assertEquals(expected, actual)
    }

    @Test
    fun `convert to achievement overview EXPECT reward transfer is the same`() {
        val achievementOverview =
            toAchievementsOverview(megaAchievementsDetails = megaAchievementsDetails)

        val expected = rewardedTransfer
        val actual =
            achievementOverview.awardedAchievements.first { it.awardId == awardId }.rewardedTransferInBytes

        assertEquals(expected, actual)
    }

    @Test
    fun `convert to achievement overview EXPECT storage from referrals is the same`() {
        val achievementOverview =
            toAchievementsOverview(megaAchievementsDetails = megaAchievementsDetails)

        val expected = storageFromReferrals
        val actual =
            achievementOverview.achievedStorageFromReferralsInBytes

        assertEquals(expected, actual)
    }

    @Test
    fun `convert to achievement overview EXPECT transfer from referrals is the same`() {
        val achievementOverview =
            toAchievementsOverview(megaAchievementsDetails = megaAchievementsDetails)

        val expected = transferFromReferrals
        val actual =
            achievementOverview.achievedTransferFromReferralsInBytes

        assertEquals(expected, actual)
    }

    @Test
    fun `convert invite type of awarded achievement EXPECT have referral emails`() {
        val megaAchievementsDetails = mock<MegaAchievementsDetails> {
            on { getAwardEmails(index) }.thenReturn(
                emailMegaStringList)
            on { getAwardClass(index) }.thenReturn(MEGA_ACHIEVEMENT_INVITE.classValue)
            on { getAwardExpirationTs(index) }.thenReturn(expirationTimestamp)
            on { getClassStorage(MEGA_ACHIEVEMENT_INVITE.classValue) }.thenReturn(
                rewardedStorage)
            on { getClassTransfer(MEGA_ACHIEVEMENT_INVITE.classValue) }.thenReturn(
                rewardedTransfer)
            on { getClassExpire(MEGA_ACHIEVEMENT_INVITE.classValue) }.thenReturn(
                expirationTimestamp.toInt())
            on { awardsCount }.thenReturn(1)
            on { getAwardId(index) }.thenReturn(awardId)
            on { getRewardAwardId(index) }.thenReturn(
                awardId)
            on { getRewardStorageByAwardId(awardId) }.thenReturn(rewardedStorage)
            on { getRewardTransferByAwardId(awardId) }.thenReturn(rewardedTransfer)
        }
        val achievementOverview =
            toAchievementsOverview(megaAchievementsDetails = megaAchievementsDetails)

        val expected = listOf(
            "john@yopmail.com",
            "doe@yopmail.com"
        )
        val actual =
            achievementOverview.awardedAchievements.filterIsInstance<AwardedAchievementInvite>()
                .first()
                .referredEmails

        assertEquals(expected, actual)
    }
}