package test.mega.privacy.android.app.main.megaachievements

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.megaachievements.AchievementsInfoViewModel
import mega.privacy.android.app.main.megaachievements.AchievementsInfoViewModel.Companion.ACHIEVEMENTS_OVERVIEW
import mega.privacy.android.app.main.megaachievements.AchievementsInfoViewModel.Companion.ACHIEVEMENTS_TYPE
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.mapper.NumberOfDaysMapper
import mega.privacy.android.domain.entity.achievement.Achievement
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.AwardedAchievement
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AchievementsInfoViewModelTest {
    private lateinit var underTest: AchievementsInfoViewModel
    private val savedStateHandle = SavedStateHandle()
    private val deviceGateway = mock<DeviceGateway>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceGateway)
    }

    private fun initTestClass() {
        underTest = AchievementsInfoViewModel(savedStateHandle, NumberOfDaysMapper(deviceGateway))
    }

    @Test
    fun `test that achievements type should update with correct value from savedStateHandle`() =
        runTest {
            val expectedAchievement = AchievementType.MEGA_ACHIEVEMENT_WELCOME
            savedStateHandle[ACHIEVEMENTS_TYPE] = expectedAchievement

            initTestClass()

            underTest.uiState.test {
                assertThat(awaitItem().achievementType).isEqualTo(expectedAchievement)
            }
        }

    @Test
    fun `test that remaining days and award id should be updated with correct value when contains awarded achievements`() =
        runTest {
            val expectedAwardId = Random.nextInt(from = 1, until = 1000)
            val expectedAchievementType = AchievementType.MEGA_ACHIEVEMENT_WELCOME
            val expectedDaysLeft = Random.nextInt(from = 1, until = 1000)
            val startTime = Calendar.getInstance()
            val endTime = Calendar.getInstance().apply {
                add(Calendar.DATE, expectedDaysLeft)
            }

            whenever(deviceGateway.now).thenReturn(startTime.timeInMillis)

            savedStateHandle[ACHIEVEMENTS_TYPE] = expectedAchievementType
            savedStateHandle[ACHIEVEMENTS_OVERVIEW] =
                AchievementsOverview(
                    allAchievements = emptyList(),
                    awardedAchievements = listOf(
                        AwardedAchievement(
                            awardId = expectedAwardId,
                            type = expectedAchievementType,
                            // adding 1000ms = 1 seconds because there's a several milliseconds different
                            // when generating end time and start time
                            expirationTimestampInSeconds = TimeUnit.MILLISECONDS.toSeconds(endTime.timeInMillis + 1000),
                            rewardedStorageInBytes = 0,
                            rewardedTransferInBytes = 0
                        )
                    ),
                    currentStorageInBytes = 0,
                    achievedStorageFromReferralsInBytes = 0,
                    achievedTransferFromReferralsInBytes = 0
                )

            initTestClass()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.awardId).isEqualTo(expectedAwardId)
                assertThat(state.achievementRemainingDays).isEqualTo(expectedDaysLeft)
            }
        }

    @Test
    fun `test that awarded storage should be updated with grantStorageInBytes when award id is invalid and type not MEGA_ACHIEVEMENT_WELCOME`() =
        runTest {
            val expectedAchievementType = AchievementType.MEGA_ACHIEVEMENT_INVITE
            val expectedGrantedStorage = 126312783L

            savedStateHandle[ACHIEVEMENTS_TYPE] = expectedAchievementType
            savedStateHandle[ACHIEVEMENTS_OVERVIEW] =
                AchievementsOverview(
                    allAchievements = listOf(
                        Achievement(
                            expectedGrantedStorage,
                            0,
                            expectedAchievementType,
                            1263711231,
                        )
                    ),
                    awardedAchievements = listOf(
                        AwardedAchievement(
                            awardId = -1,
                            type = expectedAchievementType,
                            expirationTimestampInSeconds = 0,
                            rewardedStorageInBytes = 0,
                            rewardedTransferInBytes = 0
                        )
                    ),
                    currentStorageInBytes = 0,
                    achievedStorageFromReferralsInBytes = 0,
                    achievedTransferFromReferralsInBytes = 0
                )

            initTestClass()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.awardId).isEqualTo(-1)
                assertThat(state.awardStorageInBytes).isEqualTo(expectedGrantedStorage)
            }
        }

    @Test
    fun `test that awarded storage should be zero when award id is invalid and type is MEGA_ACHIEVEMENT_WELCOME`() =
        runTest {
            val expectedAchievementType = AchievementType.MEGA_ACHIEVEMENT_WELCOME
            val expectedGrantedStorage = 126312783L

            savedStateHandle[ACHIEVEMENTS_TYPE] = expectedAchievementType
            savedStateHandle[ACHIEVEMENTS_OVERVIEW] =
                AchievementsOverview(
                    allAchievements = listOf(
                        Achievement(
                            expectedGrantedStorage,
                            0,
                            expectedAchievementType,
                            1263711231,
                        )
                    ),
                    awardedAchievements = listOf(
                        AwardedAchievement(
                            awardId = -1,
                            type = expectedAchievementType,
                            expirationTimestampInSeconds = 0,
                            rewardedStorageInBytes = 0,
                            rewardedTransferInBytes = 0
                        )
                    ),
                    currentStorageInBytes = 0,
                    achievedStorageFromReferralsInBytes = 0,
                    achievedTransferFromReferralsInBytes = 0
                )

            initTestClass()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.awardId).isEqualTo(-1)
                assertThat(state.awardStorageInBytes).isEqualTo(0)
            }
        }

    @Test
    fun `test that rewarded storage should be the same as rewardedStorageInBytes when award id is valid`() =
        runTest {
            val expectedAchievementType = AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL
            val expectedRewardedStorage = 126312783L
            val expectedAwardId = Random.nextInt(from = 1, until = 1000)

            savedStateHandle[ACHIEVEMENTS_TYPE] = expectedAchievementType
            savedStateHandle[ACHIEVEMENTS_OVERVIEW] =
                AchievementsOverview(
                    allAchievements = emptyList(),
                    awardedAchievements = listOf(
                        AwardedAchievement(
                            awardId = expectedAwardId,
                            type = expectedAchievementType,
                            expirationTimestampInSeconds = 0,
                            rewardedStorageInBytes = expectedRewardedStorage,
                            rewardedTransferInBytes = 0
                        )
                    ),
                    currentStorageInBytes = 0,
                    achievedStorageFromReferralsInBytes = 0,
                    achievedTransferFromReferralsInBytes = 0
                )

            initTestClass()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.awardId).isEqualTo(expectedAwardId)
                assertThat(state.awardStorageInBytes).isEqualTo(expectedRewardedStorage)
            }
        }
}