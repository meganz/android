package mega.privacy.android.app.presentation.advertisements

import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.ump.ConsentInformation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
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

    @BeforeEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
            consentInformation,
        )
    }

    private fun initTestClass() {
        underTest = AdsViewModel(
            getFeatureFlagValueUseCase,
            consentInformation,
        )
    }

    @Test
    fun `test initial state should have null request`() {
        initTestClass()

        assertThat(underTest.request.value).isNull()
    }

    @Test
    fun `test scheduleRefreshAds when ads enabled and consent given should create ad request`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
            whenever(consentInformation.canRequestAds()).thenReturn(true)
            initTestClass()

            underTest.scheduleRefreshAds()
            delay(1000L)
            underTest.cancelRefreshAds()

            assertThat(underTest.request.value).isNotNull()
            assertThat(underTest.request.value).isInstanceOf(AdManagerAdRequest::class.java)
            verify(consentInformation).canRequestAds()
        }

    @Test
    fun `test scheduleRefreshAds when ads disabled should not create ad request`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(false)
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        initTestClass()

        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        assertThat(underTest.request.value).isNull()
    }

    @Test
    fun `test scheduleRefreshAds when consent not given should not create ad request`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(consentInformation.canRequestAds()).thenReturn(false)
        initTestClass()

        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        assertThat(underTest.request.value).isNull()
        verify(consentInformation).canRequestAds()
    }


    @Test
    fun `test cancelRefreshAds should stop periodic refresh`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        initTestClass()

        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        val requestAfterCancel = underTest.request.value

        advanceTimeBy(AdsViewModel.MINIMUM_AD_REFRESH_INTERVAL * 2)

        assertThat(underTest.request.value).isSameInstanceAs(requestAfterCancel)
    }

    @Test
    fun `test multiple scheduleRefreshAds calls should not create multiple jobs`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(true)
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        initTestClass()

        underTest.scheduleRefreshAds()
        underTest.scheduleRefreshAds()
        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        assertThat(underTest.request.value).isNotNull()
    }

    @Test
    fun `test error handling when feature flag fetch fails should disable ads`() = runTest {
        whenever(getFeatureFlagValueUseCase(any())).doThrow(RuntimeException("Feature flag error"))
        whenever(consentInformation.canRequestAds()).thenReturn(true)
        initTestClass()

        underTest.scheduleRefreshAds()
        delay(1000L)
        underTest.cancelRefreshAds()

        assertThat(underTest.request.value).isNull()
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}