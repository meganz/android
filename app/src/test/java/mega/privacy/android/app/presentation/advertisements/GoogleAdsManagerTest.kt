package mega.privacy.android.app.presentation.advertisements

import com.google.android.ump.ConsentInformation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.ShouldShowGenericCookieDialogUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoogleAdsManagerTest {
    private lateinit var underTest: GoogleAdsManager

    private val consentInformation = mock<ConsentInformation>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()
    private val shouldShowGenericCookieDialogUseCase = mock<ShouldShowGenericCookieDialogUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            getFeatureFlagValueUseCase,
            consentInformation,
            getCookieSettingsUseCase,
            shouldShowGenericCookieDialogUseCase
        )
    }

    private fun init() {
        underTest = GoogleAdsManager(
            consentInformation,
            getFeatureFlagValueUseCase,
            getCookieSettingsUseCase,
            shouldShowGenericCookieDialogUseCase
        )
    }

    private fun provideAdRequestParameters(): Stream<Arguments> = Stream.of(
        of(true, true, true),
        of(false, true, false),
        of(true, false, false)
    )

    @ParameterizedTest(name = "when feature flag is {0} and can request ads is {1} then result is {2}")
    @MethodSource("provideAdRequestParameters")
    fun `test that new ad request result is correct when feature flag and can request Ads are updated`(
        featureFlag: Boolean,
        canRequestAds: Boolean,
        expectedResult: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)).thenReturn(
            featureFlag
        )
        whenever(consentInformation.canRequestAds()).thenReturn(canRequestAds)
        init()
        underTest.checkForAdsAvailability()
        val request = underTest.fetchAdRequest()
        if (expectedResult) {
            assertThat(request).isNotNull()
        } else {
            assertThat(request).isNull()
        }
    }
}