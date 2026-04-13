package mega.privacy.android.shared.ads.rewarded

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class RewardedAdGateViewModelTest {

    private lateinit var underTest: RewardedAdGateViewModel
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    @BeforeEach
    fun setUp() {
        reset(getFeatureFlagValueUseCase)
    }

    private fun initViewModel() {
        underTest = RewardedAdGateViewModel(getFeatureFlagValueUseCase)
    }

    @Test
    fun `test that initial state has no dialog`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(false)
        initViewModel()
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.showDialog).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.skipAdEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that init checks eligibility and marks checking complete when flag is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(true)

            initViewModel()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.isCheckingEligibility).isFalse()
            assertThat(state.isEligible).isTrue()
        }

    @Test
    fun `test that init checks eligibility and marks checking complete when flag is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(false)

            initViewModel()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.isCheckingEligibility).isFalse()
            assertThat(state.isEligible).isFalse()
        }

    @Test
    fun `test that init marks ineligible when flag fetch fails`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds))
            .thenThrow(RuntimeException("Flag fetch failed"))

        initViewModel()
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.isCheckingEligibility).isFalse()
        assertThat(state.isEligible).isFalse()
    }

    @Test
    fun `test that requestShowDialog shows dialog when eligible`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(true)
        initViewModel()
        advanceUntilIdle()

        underTest.requestShowDialog()

        assertThat(underTest.uiState.value.showDialog).isTrue()
        assertThat(underTest.uiState.value.skipAdEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that requestShowDialog triggers skipAdEvent when not eligible`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(false)
        initViewModel()
        advanceUntilIdle()

        underTest.requestShowDialog()

        assertThat(underTest.uiState.value.showDialog).isFalse()
        assertThat(underTest.uiState.value.skipAdEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that requestShowDialog triggers skipAdEvent when eligibility check is still in progress`() =
        runTest {
            // Suspend the flag fetch indefinitely so the eligibility check never completes.
            whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).doSuspendableAnswer {
                awaitCancellation()
            }
            initViewModel()

            underTest.requestShowDialog()

            val state = underTest.uiState.value
            assertThat(state.isCheckingEligibility).isTrue()
            assertThat(state.showDialog).isFalse()
            assertThat(state.skipAdEvent).isEqualTo(triggered)
        }

    @Test
    fun `test that onSkipAdEventConsumed resets event to consumed`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(false)
        initViewModel()
        advanceUntilIdle()
        underTest.requestShowDialog()

        underTest.onSkipAdEventConsumed()

        assertThat(underTest.uiState.value.skipAdEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that dismiss resets dialog state but preserves eligibility`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(true)
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
    }

    @Test
    fun `test that setLoading sets isLoading true`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(false)
        initViewModel()
        advanceUntilIdle()

        underTest.setLoading()

        assertThat(underTest.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `test that setLoadingComplete sets isLoading false`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(false)
        initViewModel()
        advanceUntilIdle()
        underTest.setLoading()

        underTest.setLoadingComplete()

        assertThat(underTest.uiState.value.isLoading).isFalse()
    }
}
