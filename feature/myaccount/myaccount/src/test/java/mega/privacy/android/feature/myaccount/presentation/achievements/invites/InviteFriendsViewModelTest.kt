package mega.privacy.android.feature.myaccount.presentation.achievements.invites

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.achievement.Achievement
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteFriendsViewModelTest {
    private lateinit var underTest: InviteFriendsViewModel
    private val reward100Mb = 104857600L
    private val expirationInDays = 100
    private val achievementsMock = AchievementsOverview(
        allAchievements = listOf(
            Achievement(
                reward100Mb,
                0L,
                AchievementType.MEGA_ACHIEVEMENT_INVITE,
                expirationInDays,
            )
        ),
        awardedAchievements = emptyList(),
        currentStorageInBytes = 64716327836L,
        achievedStorageFromReferralsInBytes = reward100Mb,
        achievedTransferFromReferralsInBytes = reward100Mb
    )

    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase =
        mock()

    @AfterEach
    fun tearDown() {
        reset(getAccountAchievementsOverviewUseCase)
    }

    @Test
    fun `test that init fetches use case and updates ui state when no referral storage value is provided`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(achievementsMock)
            initTestClass(grantStorageInBytes = 0L)

            underTest.uiState.test {
                awaitItem().let {
                    assertThat(it.grantStorageInBytes).isEqualTo(reward100Mb)
                    assertThat(it.durationInDays).isEqualTo(expirationInDays)
                }
            }
        }

    @Test
    fun `test that init does not call getAccountAchievementsOverviewUseCase when referral storage value exists`() =
        runTest {
            initTestClass(grantStorageInBytes = reward100Mb)

            verify(getAccountAchievementsOverviewUseCase, never()).invoke()
            underTest.uiState.test {
                assertThat(awaitItem().grantStorageInBytes).isEqualTo(reward100Mb)
            }
        }

    private fun initTestClass(grantStorageInBytes: Long) {
        underTest = InviteFriendsViewModel(
            grantStorageInBytes = grantStorageInBytes,
            getAccountAchievementsOverviewUseCase = getAccountAchievementsOverviewUseCase,
        )
    }
}
