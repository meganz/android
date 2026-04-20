package mega.privacy.android.shared.ads.rewarded

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.advertisements.IncrementRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.advertisements.ResetRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class RewardedAdGateViewModelTest {

    private lateinit var underTest: RewardedAdGateViewModel
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val monitorRewardedAdAttemptCountUseCase: MonitorRewardedAdAttemptCountUseCase = mock {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val incrementRewardedAdAttemptCountUseCase: IncrementRewardedAdAttemptCountUseCase =
        mock()
    private val resetRewardedAdAttemptCountUseCase: ResetRewardedAdAttemptCountUseCase = mock()

    @BeforeEach
    fun setUp() {
        reset(
            getFeatureFlagValueUseCase,
            monitorRewardedAdAttemptCountUseCase,
            incrementRewardedAdAttemptCountUseCase,
            resetRewardedAdAttemptCountUseCase,
        )
        whenever(monitorRewardedAdAttemptCountUseCase()).thenReturn(emptyFlow())
    }

    private suspend fun commonStub(
        googleAdsEnabled: Boolean = true,
        rewardedAdsEnabled: Boolean = true,
        attemptCount: Int? = null,
    ) {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag))
            .thenReturn(googleAdsEnabled)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds))
            .thenReturn(rewardedAdsEnabled)
        if (attemptCount != null) {
            whenever(monitorRewardedAdAttemptCountUseCase()).thenReturn(flowOf(attemptCount))
        }
    }

    private fun initViewModel() {
        underTest = RewardedAdGateViewModel(
            getFeatureFlagValueUseCase,
            monitorRewardedAdAttemptCountUseCase,
            incrementRewardedAdAttemptCountUseCase,
            resetRewardedAdAttemptCountUseCase,
        )
    }

    @Test
    fun `test that initial state has no dialog`() = runTest {
        commonStub(rewardedAdsEnabled = false)
        initViewModel()
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.showDialog).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.skipAdEvent).isEqualTo(consumed)
        assertThat(state.currentAttemptCount).isEqualTo(0)
    }

    @Test
    fun `test that init checks eligibility and marks checking complete when both flags are enabled`() =
        runTest {
            commonStub()

            initViewModel()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.isCheckingEligibility).isFalse()
            assertThat(state.isEligible).isTrue()
        }

    @Test
    fun `test that init marks ineligible when RewardedAds flag is disabled`() =
        runTest {
            commonStub(rewardedAdsEnabled = false)

            initViewModel()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.isCheckingEligibility).isFalse()
            assertThat(state.isEligible).isFalse()
        }

    @Test
    fun `test that init marks ineligible when GoogleAdsFeatureFlag is disabled`() =
        runTest {
            commonStub(googleAdsEnabled = false)

            initViewModel()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.isCheckingEligibility).isFalse()
            assertThat(state.isEligible).isFalse()
        }

    @Test
    fun `test that init marks ineligible when flag fetch fails`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag))
            .thenReturn(true)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds))
            .thenThrow(RuntimeException("Flag fetch failed"))

        initViewModel()
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.isCheckingEligibility).isFalse()
        assertThat(state.isEligible).isFalse()
    }

    @Test
    fun `test that init collects attempt count and updates state`() = runTest {
        commonStub(attemptCount = 3)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.currentAttemptCount).isEqualTo(3)
    }

    @Test
    fun `test that requestShowDialog skips and increments when eligible and count below threshold`() =
        runTest {
            commonStub(attemptCount = 0)
            initViewModel()
            advanceUntilIdle()

            underTest.requestShowDialog()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.showDialog).isFalse()
            assertThat(underTest.uiState.value.skipAdEvent).isEqualTo(triggered)
            verify(incrementRewardedAdAttemptCountUseCase).invoke()
        }

    @Test
    fun `test that requestShowDialog shows dialog and increments when count would reach threshold`() =
        runTest {
            commonStub(attemptCount = 4)
            initViewModel()
            advanceUntilIdle()

            underTest.requestShowDialog()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.showDialog).isTrue()
            assertThat(underTest.uiState.value.skipAdEvent).isEqualTo(consumed)
            verify(incrementRewardedAdAttemptCountUseCase).invoke()
        }

    @Test
    fun `test that requestShowDialog shows dialog when count exceeds threshold`() = runTest {
        commonStub(attemptCount = 6)
        initViewModel()
        advanceUntilIdle()

        underTest.requestShowDialog()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.showDialog).isTrue()
    }

    @Test
    fun `test that requestShowDialog triggers skipAdEvent and does not increment when not eligible`() =
        runTest {
            commonStub(rewardedAdsEnabled = false, attemptCount = 10)
            initViewModel()
            advanceUntilIdle()

            underTest.requestShowDialog()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.showDialog).isFalse()
            assertThat(underTest.uiState.value.skipAdEvent).isEqualTo(triggered)
            verify(incrementRewardedAdAttemptCountUseCase, never()).invoke()
        }

    @Test
    fun `test that requestShowDialog does not increment when eligibility check is still in progress`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag))
                .doSuspendableAnswer { awaitCancellation() }
            initViewModel()

            underTest.requestShowDialog()

            val state = underTest.uiState.value
            assertThat(state.isCheckingEligibility).isTrue()
            assertThat(state.showDialog).isFalse()
            assertThat(state.skipAdEvent).isEqualTo(triggered)
            verify(incrementRewardedAdAttemptCountUseCase, never()).invoke()
        }

    @Test
    fun `test that resetAttemptCount calls reset use case`() = runTest {
        commonStub()
        initViewModel()
        advanceUntilIdle()

        underTest.resetAttemptCount()
        advanceUntilIdle()

        verify(resetRewardedAdAttemptCountUseCase).invoke()
    }

    @Test
    fun `test that onSkipAdEventConsumed resets event to consumed`() = runTest {
        commonStub(rewardedAdsEnabled = false)
        initViewModel()
        advanceUntilIdle()
        underTest.requestShowDialog()

        underTest.onSkipAdEventConsumed()

        assertThat(underTest.uiState.value.skipAdEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that dismiss resets dialog state but preserves eligibility and count`() = runTest {
        commonStub(attemptCount = 5)
        initViewModel()
        advanceUntilIdle()
        underTest.requestShowDialog()
        underTest.setLoading()

        underTest.dismiss()

        val state = underTest.uiState.value
        assertThat(state.showDialog).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.skipAdEvent).isEqualTo(consumed)
        assertThat(state.isCheckingEligibility).isFalse()
        assertThat(state.isEligible).isTrue()
        assertThat(state.currentAttemptCount).isEqualTo(5)
    }

    @Test
    fun `test that setLoading sets isLoading true`() = runTest {
        commonStub(rewardedAdsEnabled = false)
        initViewModel()
        advanceUntilIdle()

        underTest.setLoading()

        assertThat(underTest.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `test that setLoadingComplete sets isLoading false`() = runTest {
        commonStub(rewardedAdsEnabled = false)
        initViewModel()
        advanceUntilIdle()
        underTest.setLoading()

        underTest.setLoadingComplete()

        assertThat(underTest.uiState.value.isLoading).isFalse()
    }
}
