package test.mega.privacy.android.app.presentation.achievements.invites

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.achievements.invites.InviteFriendsViewModel
import mega.privacy.android.app.presentation.achievements.invites.storageBonusInBytesArg
import mega.privacy.android.domain.entity.achievement.Achievement
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteFriendsViewModelTest {
    private lateinit var underTest: InviteFriendsViewModel
    private val savedStateHandle = SavedStateHandle()
    private val reward100Mb = 104857600L
    private val expirationInDays = 100
    private val achievementsMock = AchievementsOverview(
        allAchievements = listOf(
            Achievement(
                reward100Mb,
                0L,
                AchievementType.MEGA_ACHIEVEMENT_INVITE,
                expirationInDays
            )
        ),
        awardedAchievements = emptyList(),
        currentStorageInBytes = 64716327836L,
        achievedStorageFromReferralsInBytes = reward100Mb,
        achievedTransferFromReferralsInBytes = reward100Mb
    )

    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase =
        mock()

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
        reset(getAccountAchievementsOverviewUseCase)
    }

    @Test
    fun `test that on view model init should fetch use case and update ui state correctly`() =
        runTest {
            savedStateHandle[storageBonusInBytesArg] = 0L

            initMocks()
            initTestClass()

            underTest.uiState.test {
                assertThat(awaitItem().grantStorageInBytes).isEqualTo(reward100Mb)
            }
        }

    @Test
    fun `test that getAccountAchievementsOverviewUseCase should not be called when referral storage value from saved state exists`() =
        runTest {
            savedStateHandle[storageBonusInBytesArg] = reward100Mb

            initTestClass()

            verify(getAccountAchievementsOverviewUseCase, never()).invoke()
            underTest.uiState.test {
                assertThat(awaitItem().grantStorageInBytes).isEqualTo(reward100Mb)
            }
        }

    private fun initTestClass() {
        underTest = InviteFriendsViewModel(savedStateHandle, getAccountAchievementsOverviewUseCase)
    }

    private suspend fun initMocks() {
        whenever(getAccountAchievementsOverviewUseCase()).doReturn(achievementsMock)
    }
}