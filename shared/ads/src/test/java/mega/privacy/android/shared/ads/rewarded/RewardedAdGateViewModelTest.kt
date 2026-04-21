package mega.privacy.android.shared.ads.rewarded

import app.cash.turbine.test
import com.google.android.ump.ConsentInformation
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.domain.usecase.advertisements.IncrementRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorGoogleConsentLoadedUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.advertisements.ResetRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.RewardedAdGateActionRequestedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
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
    private val consentInformation: ConsentInformation = mock()
    private val monitorGoogleConsentLoadedUseCase: MonitorGoogleConsentLoadedUseCase = mock {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase = mock {
        on { invoke() }.thenReturn(emptyFlow())
    }

    @BeforeEach
    fun setUp() {
        reset(
            getFeatureFlagValueUseCase,
            monitorRewardedAdAttemptCountUseCase,
            incrementRewardedAdAttemptCountUseCase,
            resetRewardedAdAttemptCountUseCase,
            consentInformation,
            monitorGoogleConsentLoadedUseCase,
            monitorUpdateUserDataUseCase,
        )
        whenever(monitorRewardedAdAttemptCountUseCase()).thenReturn(emptyFlow())
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(emptyFlow())
        whenever(monitorUpdateUserDataUseCase()).thenReturn(emptyFlow())
    }

    private suspend fun commonStub(
        googleAdsEnabled: Boolean = true,
        rewardedAdsEnabled: Boolean = true,
        canRequestAds: Boolean = true,
        attemptCount: Int? = null,
    ) {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag))
            .thenReturn(googleAdsEnabled)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds))
            .thenReturn(rewardedAdsEnabled)
        whenever(consentInformation.canRequestAds()).thenReturn(canRequestAds)
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(true))
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
            consentInformation,
            monitorGoogleConsentLoadedUseCase,
            monitorUpdateUserDataUseCase,
        )
    }

    @Test
    fun `test that initial state has no dialog`() = runTest {
        commonStub(rewardedAdsEnabled = false)
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showDialog).isFalse()
            assertThat(state.isAdLoading).isFalse()
            assertThat(state.skipAdEvent).isEqualTo(consumed)
            assertThat(state.currentAttemptCount).isEqualTo(0)
        }
    }

    @Test
    fun `test that init checks eligibility and marks checking complete when both flags are enabled`() =
        runTest {
            commonStub()
            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isFeatureFlagEnabled).isTrue()
                assertThat(state.isGoogleConsentLoaded).isTrue()
                assertThat(state.canRequestAds).isTrue()
            }
        }

    @Test
    fun `test that init marks ineligible when RewardedAds flag is disabled`() =
        runTest {
            commonStub(rewardedAdsEnabled = false)
            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isFeatureFlagEnabled).isFalse()
                assertThat(state.isGoogleConsentLoaded).isTrue()
            }
        }

    @Test
    fun `test that init marks ineligible when GoogleAdsFeatureFlag is disabled`() =
        runTest {
            commonStub(googleAdsEnabled = false)
            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isFeatureFlagEnabled).isFalse()
                assertThat(state.isGoogleConsentLoaded).isTrue()
            }
        }

    @Test
    fun `test that init marks ineligible when flag fetch fails`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag))
            .thenReturn(true)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds))
            .thenThrow(RuntimeException("Flag fetch failed"))
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(true))

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFeatureFlagEnabled).isFalse()
            assertThat(state.isGoogleConsentLoaded).isTrue()
        }
    }

    @Test
    fun `test that init collects attempt count and updates state`() = runTest {
        commonStub(attemptCount = 3)
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().currentAttemptCount).isEqualTo(3)
        }
    }

    @Test
    fun `test that requestShowDialog skips and increments when eligible and count below threshold`() =
        runTest {
            commonStub(attemptCount = 0)
            initViewModel()

            underTest.uiState.test {
                awaitItem() // settled init state
                underTest.requestShowDialog()

                val state = awaitItem()
                assertThat(state.showDialog).isFalse()
                assertThat(state.skipAdEvent).isEqualTo(triggered)
                verify(incrementRewardedAdAttemptCountUseCase).invoke()
            }
        }

    @Test
    fun `test that requestShowDialog shows dialog and increments when count would reach threshold`() =
        runTest {
            commonStub(attemptCount = 4)
            initViewModel()

            underTest.uiState.test {
                awaitItem() // settled init state
                underTest.requestShowDialog()

                val state = awaitItem()
                assertThat(state.showDialog).isTrue()
                assertThat(state.skipAdEvent).isEqualTo(consumed)
                verify(incrementRewardedAdAttemptCountUseCase).invoke()
            }
        }

    @Test
    fun `test that requestShowDialog shows dialog when count exceeds threshold`() = runTest {
        commonStub(attemptCount = 6)
        initViewModel()

        underTest.uiState.test {
            awaitItem() // settled init state
            underTest.requestShowDialog()

            assertThat(awaitItem().showDialog).isTrue()
        }
    }

    @Test
    fun `test that requestShowDialog triggers skipAdEvent and does not increment when not eligible`() =
        runTest {
            commonStub(rewardedAdsEnabled = false, attemptCount = 10)
            initViewModel()

            underTest.uiState.test {
                awaitItem() // settled init state
                underTest.requestShowDialog()

                val state = awaitItem()
                assertThat(state.showDialog).isFalse()
                assertThat(state.skipAdEvent).isEqualTo(triggered)
                verify(incrementRewardedAdAttemptCountUseCase, never()).invoke()
            }
        }

    @Test
    fun `test that requestShowDialog does not increment when feature flag check is still in progress`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag))
                .doSuspendableAnswer { awaitCancellation() }
            initViewModel()

            underTest.uiState.test {
                awaitItem() // default state (flag check suspended)
                underTest.requestShowDialog()

                val state = awaitItem()
                assertThat(state.isFeatureFlagEnabled).isFalse()
                assertThat(state.showDialog).isFalse()
                assertThat(state.skipAdEvent).isEqualTo(triggered)
                verify(incrementRewardedAdAttemptCountUseCase, never()).invoke()
            }
        }

    @Test
    fun `test that resetAttemptCount calls reset use case`() = runTest {
        commonStub()
        initViewModel()

        underTest.resetAttemptCount()

        verify(resetRewardedAdAttemptCountUseCase).invoke()
    }

    @Test
    fun `test that onSkipAdEventConsumed resets event to consumed`() = runTest {
        commonStub(rewardedAdsEnabled = false)
        initViewModel()

        underTest.uiState.test {
            awaitItem() // settled init state
            underTest.requestShowDialog()
            assertThat(awaitItem().skipAdEvent).isEqualTo(triggered)

            underTest.onSkipAdEventConsumed()
            assertThat(awaitItem().skipAdEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that dismiss resets dialog state but preserves eligibility and count`() = runTest {
        commonStub(attemptCount = 5)
        initViewModel()

        underTest.uiState.test {
            awaitItem() // settled init state
            underTest.requestShowDialog()
            awaitItem() // dialog shown
            underTest.setAdLoading()
            awaitItem() // ad loading

            underTest.dismiss()

            val state = awaitItem()
            assertThat(state.showDialog).isFalse()
            assertThat(state.isAdLoading).isFalse()
            assertThat(state.skipAdEvent).isEqualTo(consumed)
            assertThat(state.isFeatureFlagEnabled).isTrue()
            assertThat(state.canRequestAds).isTrue()
            assertThat(state.currentAttemptCount).isEqualTo(5)
        }
    }

    @Test
    fun `test that setAdLoading sets isLoading true`() = runTest {
        commonStub(rewardedAdsEnabled = false)
        initViewModel()

        underTest.uiState.test {
            awaitItem() // settled init state

            underTest.setAdLoading()
            assertThat(awaitItem().isAdLoading).isTrue()
        }
    }

    @Test
    fun `test that setAdLoadingComplete sets isLoading false`() = runTest {
        commonStub(rewardedAdsEnabled = false)
        initViewModel()

        underTest.uiState.test {
            awaitItem() // settled init state
            underTest.setAdLoading()
            assertThat(awaitItem().isAdLoading).isTrue()

            underTest.setAdLoadingComplete()
            assertThat(awaitItem().isAdLoading).isFalse()
        }
    }

    @Test
    fun `test that init marks ineligible when consent cannot request ads`() = runTest {
        commonStub(canRequestAds = false)
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFeatureFlagEnabled).isTrue()
            assertThat(state.isGoogleConsentLoaded).isTrue()
            assertThat(state.canRequestAds).isFalse()
        }
    }

    @Test
    fun `test that consent not loaded keeps isGoogleConsentLoaded false`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(true)
        // monitorGoogleConsentLoadedUseCase returns emptyFlow() by default (never emits)

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFeatureFlagEnabled).isTrue()
            assertThat(state.isGoogleConsentLoaded).isFalse()
        }
    }

    @Test
    fun `test that consent loaded updates isGoogleConsentLoaded and canRequestAds`() = runTest {
        val consentFlow = MutableSharedFlow<Boolean>()
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(consentFlow)
        whenever(consentInformation.canRequestAds()).thenReturn(false)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(true)

        initViewModel()

        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.isFeatureFlagEnabled).isTrue()
            assertThat(initial.isGoogleConsentLoaded).isFalse()
            assertThat(initial.canRequestAds).isFalse()

            whenever(consentInformation.canRequestAds()).thenReturn(true)
            consentFlow.emit(true)

            val state = awaitItem()
            assertThat(state.isGoogleConsentLoaded).isTrue()
            assertThat(state.canRequestAds).isTrue()
        }
    }

    @Test
    fun `test that user data update resets feature flag and rechecks`() = runTest {
        val userDataFlow = MutableSharedFlow<Unit>()
        whenever(monitorUpdateUserDataUseCase()).thenReturn(userDataFlow)
        commonStub()
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().isFeatureFlagEnabled).isTrue()

            // Change flag to disabled
            whenever(getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)).thenReturn(false)

            // drop(1) means first emission is ignored, second triggers recheck
            userDataFlow.emit(Unit)
            userDataFlow.emit(Unit)

            assertThat(awaitItem().isFeatureFlagEnabled).isFalse()
        }
    }

    @Test
    fun `test that requestShowDialog skips when consent not given even with flags enabled`() =
        runTest {
            commonStub(canRequestAds = false, attemptCount = 10)
            initViewModel()

            underTest.uiState.test {
                awaitItem() // settled init state
                underTest.requestShowDialog()

                val state = awaitItem()
                assertThat(state.showDialog).isFalse()
                assertThat(state.skipAdEvent).isEqualTo(triggered)
                verify(incrementRewardedAdAttemptCountUseCase, never()).invoke()
            }
        }

    @Test
    fun `test that requestShowDialog tracks RewardedAdGateActionRequestedEvent when eligible`() =
        runTest {
            commonStub(attemptCount = 0)
            initViewModel()
            advanceUntilIdle()

            underTest.requestShowDialog()

            assertThat(analyticsExtension.events)
                .contains(RewardedAdGateActionRequestedEvent)
        }

    @Test
    fun `test that requestShowDialog does not track RewardedAdGateActionRequestedEvent when not eligible`() =
        runTest {
            commonStub(rewardedAdsEnabled = false, attemptCount = 0)
            initViewModel()
            advanceUntilIdle()

            underTest.requestShowDialog()

            assertThat(analyticsExtension.events)
                .doesNotContain(RewardedAdGateActionRequestedEvent)
        }

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()
    }
}
