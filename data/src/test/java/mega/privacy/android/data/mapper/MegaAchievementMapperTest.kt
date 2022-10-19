package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.achievement.AchievementType
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaStringList
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

class MegaAchievementMapperTest {

    private val megaStringList = mock<MegaStringList> {
        on { get(0) }.thenReturn("john@yopmail.com")
        on { get(1) }.thenReturn("doe@yopmail.com")
        on { size() }.thenReturn(2)
    }

    private val megaAchievementsDetails = mock<MegaAchievementsDetails> {
        on { getAwardEmails(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue.toLong()) }.thenReturn(
            megaStringList)
        on { getClassStorage(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue) }.thenReturn(
            3612863212)
        on { getClassTransfer(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue) }.thenReturn(
            756756756754)
        on { awardsCount }.thenReturn(5)
        on { getAwardId(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue.toLong()) }.thenReturn(1)
        on { getAwardExpirationTs(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue.toLong()) }.thenReturn(
            45435423323242)
        on { getRewardAwardId(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue.toLong()) }.thenReturn(
            AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue)
        on { getRewardStorageByAwardId(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue) }.thenReturn(
            443432442)
        on { getRewardTransferByAwardId(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue) }.thenReturn(
            6543234324324)
    }

    @Test
    fun `test that data from mega achievement details is correctly mapped to mapper`() {
        val actual = toMegaAchievement(megaAchievementsDetails = megaAchievementsDetails,
            achievementType = AchievementType.MEGA_ACHIEVEMENT_WELCOME,
            awardIndex = AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue.toLong())
        assertEquals(megaAchievementsDetails.getClassStorage(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue),
            3612863212)
        assertEquals(megaAchievementsDetails.getClassTransfer(AchievementType.MEGA_ACHIEVEMENT_WELCOME.classValue),
            756756756754)
        assertEquals(megaAchievementsDetails.awardsCount, actual.unlockedAwardsCount)
    }
}