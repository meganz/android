package mega.privacy.android.app.presentation.achievements.info

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.mapper.NumberOfDaysMapper
import mega.privacy.android.domain.entity.achievement.Achievement
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.AwardedAchievement
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsInfoViewModelTest {
    private lateinit var underTest: AchievementsInfoViewModel
    private var savedStateHandle = SavedStateHandle()
    private val deviceGateway = mock<DeviceGateway>()
    private val getAccountAchievementsOverviewUseCase =
        mock<GetAccountAchievementsOverviewUseCase>()


    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        reset(deviceGateway, getAccountAchievementsOverviewUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel(
        achievementsOverview: AchievementsOverview = AchievementsOverview(
            allAchievements = emptyList(),
            awardedAchievements = emptyList(),
            currentStorageInBytes = 0,
            achievedStorageFromReferralsInBytes = 0,
            achievedTransferFromReferralsInBytes = 0,
        ),
    ) {
        getAccountAchievementsOverviewUseCase.stub {
            onBlocking { invoke() }.thenReturn(achievementsOverview)
        }

        underTest = AchievementsInfoViewModel(
            savedStateHandle = savedStateHandle,
            getAccountAchievementsOverviewUseCase = getAccountAchievementsOverviewUseCase,
            numberOfDaysMapper = NumberOfDaysMapper(deviceGateway)
        )
    }

    @Test
    fun `test that achievements type should update with correct value from savedStateHandle`() =
        runTest {
            val expectedAchievement = AchievementType.MEGA_ACHIEVEMENT_WELCOME

            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = AchievementMain(achievementType = expectedAchievement)
            )

            initViewModel()

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
            // adding 1000ms = 1 seconds because there's a several milliseconds different
            // when generating end time and start time
            val endTime =
                startTime.timeInMillis + TimeUnit.DAYS.toMillis(expectedDaysLeft.toLong()) + 1000

            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = AchievementMain(achievementType = expectedAchievementType)
            )

            whenever(deviceGateway.now).thenReturn(startTime.timeInMillis)
            initViewModel(
                achievementsOverview =
                    AchievementsOverview(
                        allAchievements = emptyList(),
                        awardedAchievements = listOf(
                            AwardedAchievement(
                                awardId = expectedAwardId,
                                type = expectedAchievementType,
                                expirationTimestampInSeconds = TimeUnit.MILLISECONDS.toSeconds(
                                    endTime
                                ),
                                rewardedStorageInBytes = 0,
                                rewardedTransferInBytes = 0
                            )
                        ),
                        currentStorageInBytes = 0,
                        achievedStorageFromReferralsInBytes = 0,
                        achievedTransferFromReferralsInBytes = 0
                    )
            )

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

            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = AchievementMain(achievementType = expectedAchievementType)
            )
            initViewModel(
                achievementsOverview =
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
            )

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

            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = AchievementMain(achievementType = expectedAchievementType)
            )

            initViewModel(
                achievementsOverview =
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
            )

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

            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = AchievementMain(achievementType = expectedAchievementType)
            )

            initViewModel(
                achievementsOverview =
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
            )

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.awardId).isEqualTo(expectedAwardId)
                assertThat(state.awardStorageInBytes).isEqualTo(expectedRewardedStorage)
            }
        }

    @Test
    fun `test that durationInDays are updated correctly`() =
        runTest {
            val expectedAchievementType = AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL
            val mockDurationInDays = 365

            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = AchievementMain(achievementType = expectedAchievementType)
            )

            initViewModel(
                achievementsOverview =
                    AchievementsOverview(
                        allAchievements = listOf(
                            mock<Achievement> {
                                on { type }.thenReturn(expectedAchievementType)
                                on { durationInDays }.thenReturn(mockDurationInDays)
                            }
                        ),
                        awardedAchievements = emptyList(),
                        currentStorageInBytes = 0,
                        achievedStorageFromReferralsInBytes = 0,
                        achievedTransferFromReferralsInBytes = 0
                    )
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.durationInDays).isEqualTo(mockDurationInDays)
            }
        }
}