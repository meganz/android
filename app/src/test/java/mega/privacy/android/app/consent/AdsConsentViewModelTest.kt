package mega.privacy.android.app.consent

import android.app.Activity
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.consent.model.AdsConsentState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.advertisements.SetGoogleConsentLoadedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdsConsentViewModelTest {
    private lateinit var underTest: AdsConsentViewModel

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val adConsentWrapper = mock<AdConsentWrapper>()
    private val setGoogleConsentLoadedUseCase = mock<SetGoogleConsentLoadedUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = AdsConsentViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            adConsentWrapper = adConsentWrapper,
            setGoogleConsentLoadedUseCase = setGoogleConsentLoadedUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getFeatureFlagValueUseCase,
            adConsentWrapper,
            setGoogleConsentLoadedUseCase,
        )
    }

    @Test
    fun `test that ad feature disabled event is triggered if feature flag is not enabled`() =
        runTest {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(ApiFeatures.GoogleAdsFeatureFlag) } doReturn false
            }

            underTest.state.test {
                val actual = awaitItem() as AdsConsentState.Data
                assertThat(actual.adFeatureDisabled).isEqualTo(triggered)
                assertThat(actual.adConsentHandledEvent).isEqualTo(consumed)
                assertThat(actual.showConsentFormEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that adConsentHandled event is triggered if consent flow returns false`() = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.GoogleAdsFeatureFlag) } doReturn true
        }
        adConsentWrapper.stub {
            on { getCanRequestConsentFlow(any()) } doReturn flow {
                emit(false)
                awaitCancellation()
            }
        }

        underTest.onLoaded(mock<Activity>())

        underTest.state.test {
            val actual = awaitItem() as AdsConsentState.Data
            assertThat(actual.adFeatureDisabled).isEqualTo(consumed)
            assertThat(actual.adConsentHandledEvent).isEqualTo(triggered)
            assertThat(actual.showConsentFormEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that showConsentForm event is triggered if consent flow returns true`() = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.GoogleAdsFeatureFlag) } doReturn true
        }
        adConsentWrapper.stub {
            on { getCanRequestConsentFlow(any()) } doReturn flow {
                emit(true)
                awaitCancellation()
            }
        }

        underTest.state.test {
            assertThat(awaitItem()).isInstanceOf(AdsConsentState.Loading::class.java)
            underTest.onLoaded(mock<Activity>())
            val actual = awaitItem() as AdsConsentState.Data
            assertThat(actual.adFeatureDisabled).isEqualTo(consumed)
            assertThat(actual.adConsentHandledEvent).isEqualTo(consumed)
            assertThat(actual.showConsentFormEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that showConsentForm event is consumed if onConsentFormDisplayed is called`() =
        runTest {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(ApiFeatures.GoogleAdsFeatureFlag) } doReturn true
            }
            adConsentWrapper.stub {
                on { getCanRequestConsentFlow(any()) } doReturn flow {
                    emit(true)
                    awaitCancellation()
                }
            }

            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(AdsConsentState.Loading::class.java)
                underTest.onLoaded(mock<Activity>())
                advanceUntilIdle()
                val triggeredEvent = awaitItem() as AdsConsentState.Data
                assertWithMessage("Show consent form is expected to be triggered").that(
                    triggeredEvent.showConsentFormEvent
                ).isEqualTo(triggered)
                underTest.onConsentFormDisplayed()
                val actual = awaitItem() as AdsConsentState.Data
                assertThat(actual.adFeatureDisabled).isEqualTo(consumed)
                assertThat(actual.adConsentHandledEvent).isEqualTo(consumed)
                assertWithMessage("Show consent form is expected to be consumed").that(actual.showConsentFormEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that adConsentHandled event is triggered if onConsentSelected is called`() =
        runTest {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(ApiFeatures.GoogleAdsFeatureFlag) } doReturn true
            }
            adConsentWrapper.stub {
                on { getCanRequestConsentFlow(any()) } doReturn flow {
                    emit(true)
                    awaitCancellation()
                }
            }
            underTest.onLoaded(mock<Activity>())
            underTest.state.test {
                val initial = awaitItem() as AdsConsentState.Data
                assertThat(initial.adFeatureDisabled).isEqualTo(consumed)
                assertThat(initial.adConsentHandledEvent).isEqualTo(consumed)
                assertThat(initial.showConsentFormEvent).isEqualTo(triggered)
                underTest.onConsentSelected(null)
                val actual = awaitItem() as AdsConsentState.Data
                assertThat(actual.adFeatureDisabled).isEqualTo(consumed)
                assertThat(actual.adConsentHandledEvent).isEqualTo(triggered)
                assertThat(actual.showConsentFormEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that consent use case is called when onAdConsentHandled is called`() = runTest {
        underTest.onAdConsentHandled()

        verify(setGoogleConsentLoadedUseCase).invoke(true)
    }
}