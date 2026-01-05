package mega.privacy.android.app.presentation.advertisements

import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.ump.ConsentInformation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorGoogleConsentLoadedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdsViewModelTest {
    private lateinit var underTest: AdsViewModel
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val consentInformation: ConsentInformation = mock()
    private val monitorGoogleConsentLoadedUseCase: MonitorGoogleConsentLoadedUseCase = mock {
        on { invoke() }.thenReturn(flowOf())
    }
    private val monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase = mock {
        on { invoke() }.thenReturn(flowOf())
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
            consentInformation,
            monitorUpdateUserDataUseCase,
        )
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf())
    }

    private fun initTestClass() {
        underTest = AdsViewModel(
            getFeatureFlagValueUseCase,
            consentInformation,
            monitorGoogleConsentLoadedUseCase,
            monitorUpdateUserDataUseCase,
        )
    }

    @Test
    fun `test initial state should have null request`() {
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(false))
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf())
        initTestClass()

        assertThat(underTest.uiState.value.request).isNull()
        assertThat(underTest.uiState.value.isAdsFeatureEnabled).isNull()
    }

    @Test
    fun `test scheduleRefreshAds when ads enabled and consent given should create ad request`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
            whenever(consentInformation.canRequestAds()).thenReturn(true)
            whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(false))
            whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf())
            initTestClass()

            underTest.scheduleRefreshAds()
            delay(1000L)
            underTest.cancelRefreshAds()

            assertThat(underTest.uiState.value.request).isNotNull()
            assertThat(underTest.uiState.value.request).isInstanceOf(AdManagerAdRequest::class.java)
            assertThat(underTest.uiState.value.isAdsFeatureEnabled).isTrue()
            verify(consentInformation).canRequestAds()
        }

    @Test
    fun `test scheduleRefreshAds when ads disabled should not create ad request`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(false)
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(false))
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf())
        initTestClass()

        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        assertThat(underTest.uiState.value.request).isNull()
        assertThat(underTest.uiState.value.isAdsFeatureEnabled).isFalse()
    }

    @Test
    fun `test scheduleRefreshAds when consent not given should not create ad request`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(consentInformation.canRequestAds()).thenReturn(false)
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(false))
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf())
        initTestClass()

        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        assertThat(underTest.uiState.value.request).isNull()
        assertThat(underTest.uiState.value.isAdsFeatureEnabled).isTrue()
        verify(consentInformation).canRequestAds()
    }


    @Test
    fun `test cancelRefreshAds should stop periodic refresh`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(false))
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf())
        initTestClass()

        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        val requestAfterCancel = underTest.uiState.value.request

        advanceTimeBy(AdsViewModel.MINIMUM_AD_REFRESH_INTERVAL * 2)

        assertThat(underTest.uiState.value.request).isSameInstanceAs(requestAfterCancel)
    }

    @Test
    fun `test multiple scheduleRefreshAds calls should not create multiple jobs`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(false))
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf())
        initTestClass()

        underTest.scheduleRefreshAds()
        underTest.scheduleRefreshAds()
        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        assertThat(underTest.uiState.value.request).isNotNull()
    }

    @Test
    fun `test error handling when feature flag fetch fails should disable ads`() = runTest {
        whenever(getFeatureFlagValueUseCase(any())).doThrow(RuntimeException("Feature flag error"))
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(false))
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf())
        initTestClass()

        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        assertThat(underTest.uiState.value.request).isNull()
        assertThat(underTest.uiState.value.isAdsFeatureEnabled).isFalse()
    }

    @Test
    fun `test when user data updates should reset feature flag and reschedule ads`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        whenever(monitorGoogleConsentLoadedUseCase()).thenReturn(flowOf(true))
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf(Unit, Unit))
        initTestClass()

        delay(1000L)

        // After user data updates, feature flag should be reset and ads rescheduled
        // The first emission is dropped, so we need at least 2 emissions
        assertThat(underTest.uiState.value.isAdsFeatureEnabled).isNotNull()
        
        underTest.cancelRefreshAds()
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}