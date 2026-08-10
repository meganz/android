package mega.privacy.android.app.presentation.achievements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.achievements.model.AwardAchievementExpirationStatus
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.achievement.Achievement
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.AwardedAchievement
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.concurrent.TimeUnit

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AchievementsOverviewViewModelTest {

    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase =
        mock()
    private val areAchievementsEnabled: AreAchievementsEnabledUseCase = mock()
    private lateinit var underTest: AchievementsOverviewViewModel

    private val fakeAchievements = AchievementsOverview(
        allAchievements = listOf(),
        awardedAchievements = listOf(),
        currentStorageInBytes = 0L,
        achievedStorageFromReferralsInBytes = 0L,
        achievedTransferFromReferralsInBytes = 0L
    )

    @BeforeEach
    fun setUp() {
        runBlocking { stubCommon() }
        initViewModel()
    }

    @AfterEach
    fun tearDown() {
        reset(
            getAccountAchievementsOverviewUseCase,
            areAchievementsEnabled
        )
    }

    private suspend fun stubCommon() {
        whenever(areAchievementsEnabled()).thenReturn(false)
        whenever(getAccountAchievementsOverviewUseCase()).thenReturn(fakeAchievements)
    }

    private fun initViewModel() {
        underTest = AchievementsOverviewViewModel(
            getAccountAchievementsOverviewUseCase = getAccountAchievementsOverviewUseCase,
            areAchievementsEnabled = areAchievementsEnabled
        )
    }

    @Test
    fun `test that state contains content when the achievements overview use case returns achievements`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(fakeAchievements)

            underTest.state.test {
                assertThat(awaitItem().achievementsOverview).isEqualTo(fakeAchievements)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state contains an error when the get achievements overview use case returns an exception`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenThrow(RuntimeException("Error"))
            initViewModel()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isInstanceOf(StateEventWithContentTriggered(R.string.cancel_subscription_error)::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that currentStorage is updated as expected`() = runTest {
        val mockCurrentStorage = 1000L
        val mockOverview = initAchievementsOverview(currentStorageInBytes = mockCurrentStorage)
        whenever(getAccountAchievementsOverviewUseCase()).thenReturn(mockOverview)

        initViewModel()
        underTest.state.test {
            assertThat(awaitItem().currentStorage).isEqualTo(mockCurrentStorage)
        }
    }

    private fun initAchievementsOverview(
        allAchievements: List<Achievement> = listOf(),
        awardedAchievements: List<AwardedAchievement> = listOf(),
        currentStorageInBytes: Long = 0,
        achievedStorageFromReferralsInBytes: Long = 0,
        achievedTransferFromReferralsInBytes: Long = 0,
    ) = AchievementsOverview(
        allAchievements = allAchievements,
        awardedAchievements = awardedAchievements,
        currentStorageInBytes = currentStorageInBytes,
        achievedStorageFromReferralsInBytes = achievedStorageFromReferralsInBytes,
        achievedTransferFromReferralsInBytes = achievedTransferFromReferralsInBytes
    )

    @ParameterizedTest(name = "is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that areAllRewardsExpired update correct when all invite achievements are expired`(
        areAllRewardsExpired: Boolean,
    ) = runTest {
        val mockAchievement: AwardedAchievement = if (areAllRewardsExpired) {
            mock<AwardedAchievementInvite> {
                on { expirationTimestampInSeconds }.thenReturn(-1)
            }
        } else {
            mock()
        }
        val mockOverview = initAchievementsOverview(awardedAchievements = listOf(mockAchievement))
        whenever(getAccountAchievementsOverviewUseCase()).thenReturn(mockOverview)

        initViewModel()
        underTest.state.test {
            assertThat(awaitItem().areAllRewardsExpired).isEqualTo(areAllRewardsExpired)
        }
    }

    @Test
    fun `test that values regarding referrals are updated as expected`() =
        runTest {
            val mockStorage = 1000L
            val mockDurationInDays = 365
            val mockAchievement = mock<Achievement> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_INVITE)
                on { grantStorageInBytes }.thenReturn(mockStorage)
                on { durationInDays }.thenReturn(mockDurationInDays)
            }
            val mockOverview = initAchievementsOverview(
                allAchievements = listOf(mockAchievement),
                achievedStorageFromReferralsInBytes = mockStorage,
            )
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(mockOverview)

            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.hasReferrals).isTrue()
                assertThat(state.referralsStorage).isEqualTo(mockStorage)
                assertThat(state.referralsAwardStorage).isEqualTo(mockStorage)
                assertThat(state.referralsDurationInDays).isEqualTo(mockDurationInDays)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that values regarding installApp are updated as expected`() =
        runTest {
            val mockStorage = 1000L
            val mockDurationInDays = 365
            val mockAchievement = mock<Achievement> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL)
                on { grantStorageInBytes }.thenReturn(mockStorage)
                on { durationInDays }.thenReturn(mockDurationInDays)
            }

            val mockAwardedAchievement = mock<AwardedAchievementInvite> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL)
                on { rewardedStorageInBytes }.thenReturn(mockStorage)
            }
            val mockOverview = initAchievementsOverview(
                allAchievements = listOf(mockAchievement),
                awardedAchievements = listOf(mockAwardedAchievement)
            )
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(mockOverview)

            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.installAppStorage).isEqualTo(mockStorage)
                assertThat(state.installAppAwardStorage).isEqualTo(mockStorage)
                assertThat(state.installAppDurationInDays).isEqualTo(mockDurationInDays)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that values regarding installDesktop are updated as expected`() =
        runTest {
            val mockStorage = 1000L
            val mockDurationInDays = 365
            val mockAchievement = mock<Achievement> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL)
                on { grantStorageInBytes }.thenReturn(mockStorage)
                on { durationInDays }.thenReturn(mockDurationInDays)
            }

            val mockAwardedAchievement = mock<AwardedAchievementInvite> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL)
                on { rewardedStorageInBytes }.thenReturn(mockStorage)
            }
            val mockOverview = initAchievementsOverview(
                allAchievements = listOf(mockAchievement),
                awardedAchievements = listOf(mockAwardedAchievement)
            )
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(mockOverview)

            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.installDesktopStorage).isEqualTo(mockStorage)
                assertThat(state.installDesktopAwardStorage).isEqualTo(mockStorage)
                assertThat(state.installDesktopDurationInDays).isEqualTo(mockDurationInDays)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that values regarding registration are updated as expected`() =
        runTest {
            val mockStorage = 1000L
            val mockAwardedAchievement = mock<AwardedAchievementInvite> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_WELCOME)
                on { rewardedStorageInBytes }.thenReturn(mockStorage)
            }
            val mockOverview = initAchievementsOverview(
                awardedAchievements = listOf(mockAwardedAchievement)
            )
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(mockOverview)

            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.hasRegistrationAward).isTrue()
                assertThat(state.registrationAwardStorage).isEqualTo(mockStorage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "when expirationTimestampInSeconds is {0}")
    @ValueSource(longs = [10000L, -1, 0])
    fun `test that values regarding mega vpn free trial are updated as expected`(
        expirationTimestampInSeconds: Long,
    ) =
        runTest {
            val mockExpirationTimes = if (expirationTimestampInSeconds > 0) {
                System.currentTimeMillis() + expirationTimestampInSeconds
            } else {
                expirationTimestampInSeconds
            }
            val mockStorage = 1000L
            val mockDurationInDays = 365
            val mockAchievement = mock<Achievement> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_MEGA_VPN_TRIAL)
                on { grantStorageInBytes }.thenReturn(mockStorage)
                on { durationInDays }.thenReturn(mockDurationInDays)
            }

            val mockAwardedAchievement = mock<AwardedAchievementInvite> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_MEGA_VPN_TRIAL)
                on { rewardedStorageInBytes }.thenReturn(mockStorage)
                on { this.expirationTimestampInSeconds }.thenReturn(mockExpirationTimes)
            }
            val mockOverview = initAchievementsOverview(
                allAchievements = listOf(mockAchievement),
                awardedAchievements = listOf(mockAwardedAchievement)
            )
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(mockOverview)

            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.hasMegaVPNTrial).isTrue()
                assertThat(state.megaVPNTrialStorage).isEqualTo(mockStorage)
                assertThat(state.megaVPNTrialAwardStorage).isEqualTo(mockStorage)
                assertThat(state.megaVPNTrialDurationInDays).isEqualTo(mockDurationInDays)
                assertThat(state.megaVPNTrialAwardDaysLeft).isEqualTo(
                    when (expirationTimestampInSeconds) {
                        -1L -> AwardAchievementExpirationStatus.Expired
                        0L -> AwardAchievementExpirationStatus.Permanent
                        else -> {
                            val expirationDate =
                                Util.calculateDateFromTimestamp(mockExpirationTimes)
                            val now = Calendar.getInstance()
                            val diffTime = expirationDate.timeInMillis - now.timeInMillis
                            val daysLeft = diffTime / TimeUnit.DAYS.toMillis(1)
                            AwardAchievementExpirationStatus.Valid(daysLeft)
                        }
                    }
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "when expirationTimestampInSeconds is {0}")
    @ValueSource(longs = [10000L, -1, 0])
    fun `test that values regarding mega pass free trial are updated as expected`(
        expirationTimestampInSeconds: Long,
    ) =
        runTest {
            val mockExpirationTimes = if (expirationTimestampInSeconds > 0) {
                System.currentTimeMillis() + expirationTimestampInSeconds
            } else {
                expirationTimestampInSeconds
            }
            val mockStorage = 1000L
            val mockDurationInDays = 365
            val mockAchievement = mock<Achievement> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_MEGA_PWM_TRIAL)
                on { grantStorageInBytes }.thenReturn(mockStorage)
                on { durationInDays }.thenReturn(mockDurationInDays)
            }

            val mockAwardedAchievement = mock<AwardedAchievementInvite> {
                on { type }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_MEGA_PWM_TRIAL)
                on { rewardedStorageInBytes }.thenReturn(mockStorage)
                on { this.expirationTimestampInSeconds }.thenReturn(mockExpirationTimes)
            }
            val mockOverview = initAchievementsOverview(
                allAchievements = listOf(mockAchievement),
                awardedAchievements = listOf(mockAwardedAchievement)
            )
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(mockOverview)

            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.hasMegaPassTrial).isTrue()
                assertThat(state.megaPassTrialStorage).isEqualTo(mockStorage)
                assertThat(state.megaPassTrialAwardStorage).isEqualTo(mockStorage)
                assertThat(state.megaPassTrialDurationInDays).isEqualTo(mockDurationInDays)
                assertThat(state.megaPassTrialAwardDaysLeft).isEqualTo(
                    when (expirationTimestampInSeconds) {
                        -1L -> AwardAchievementExpirationStatus.Expired
                        0L -> AwardAchievementExpirationStatus.Permanent
                        else -> {
                            val expirationDate =
                                Util.calculateDateFromTimestamp(mockExpirationTimes)
                            val now = Calendar.getInstance()
                            val diffTime = expirationDate.timeInMillis - now.timeInMillis
                            val daysLeft = diffTime / TimeUnit.DAYS.toMillis(1)
                            AwardAchievementExpirationStatus.Valid(daysLeft)
                        }
                    }
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
}
