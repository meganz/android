package mega.privacy.android.app.presentation.achievements.invites

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewModel
import mega.privacy.android.domain.entity.achievement.Achievement
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InviteFriendsViewModelTest {
    private lateinit var underTest: InviteFriendsViewModel
    private var savedStateHandle = SavedStateHandle()
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

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        reset(getAccountAchievementsOverviewUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that on view model init should fetch use case and update ui state correctly`() =
        runTest {
            initMocks()
            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = InviteFriends(0L)
            )
            initTestClass()

            underTest.uiState.test {
                assertThat(awaitItem().grantStorageInBytes).isEqualTo(reward100Mb)
            }
        }

    @Test
    fun `test that getAccountAchievementsOverviewUseCase should not be called when referral storage value from saved state exists`() =
        runTest {
            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = InviteFriends(reward100Mb)
            )
            initTestClass()

            verify(getAccountAchievementsOverviewUseCase, never()).invoke()
            underTest.uiState.test {
                assertThat(awaitItem().grantStorageInBytes).isEqualTo(reward100Mb)
            }
        }

    private fun initTestClass() {
        underTest = InviteFriendsViewModel(
            savedStateHandle = savedStateHandle,
            getAccountAchievementsOverviewUseCase = getAccountAchievementsOverviewUseCase,
        )
    }

    private suspend fun initMocks() {
        whenever(getAccountAchievementsOverviewUseCase()).doReturn(achievementsMock)
    }
}