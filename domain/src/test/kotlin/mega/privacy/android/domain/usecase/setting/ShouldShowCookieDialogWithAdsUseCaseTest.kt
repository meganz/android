package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShouldShowCookieDialogWithAdsUseCaseTest {

    private lateinit var underTest: ShouldShowCookieDialogWithAdsUseCase
    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val updateCookieSettingsUseCase = mock<UpdateCookieSettingsUseCase>()

    @AfterEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
            getCookieSettingsUseCase,
        )
    }

    @BeforeAll
    fun initTestClass() {
        underTest = ShouldShowCookieDialogWithAdsUseCase(
            getFeatureFlagValueUseCase,
            getCookieSettingsUseCase,
            updateCookieSettingsUseCase
        )
    }

    private fun provideTestCases() = listOf(
        Arguments.of(true, true, true, emptySet<CookieType>(), true),
        Arguments.of(false, true, true, emptySet<CookieType>(), false),
        Arguments.of(true, false, true, emptySet<CookieType>(), false),
        Arguments.of(true, true, false, emptySet<CookieType>(), false),
        Arguments.of(true, true, true, setOf(CookieType.ESSENTIAL), true),
        Arguments.of(true, true, true, setOf(CookieType.ESSENTIAL, CookieType.ADS_CHECK), false),
        Arguments.of(true, true, true, setOf(CookieType.ADS_CHECK), false),
    )

    @ParameterizedTest(name = "The visibility of cookie dialog with ads should be: {4} when in-app ads feature is: {0}, ads are: {1}, external ads are: {2}, and cookie settings is: {3}")
    @MethodSource("provideTestCases")
    fun `test that show cookie dialog with ads should return expected value when all required fields are provided`(
        inAppAdvertisementFeature: Boolean,
        isAdsEnabledFeature: Boolean,
        isExternalAdsEnabledFeature: Boolean,
        cookieSettings: Set<CookieType>,
        expected: Boolean,
    ) {
        runTest {
            whenever(getFeatureFlagValueUseCase.invoke(any())).thenReturn(
                inAppAdvertisementFeature,
                isAdsEnabledFeature,
                isExternalAdsEnabledFeature
            )
            whenever(getCookieSettingsUseCase.invoke()).thenReturn(cookieSettings)
            whenever(updateCookieSettingsUseCase.invoke(any())).thenReturn(Unit)
            val result = underTest.invoke(
                inAppAdvertisementFeature = mock(),
                isAdsEnabledFeature = mock(),
                isExternalAdsEnabledFeature = mock(),
            )

            assertThat(result).isEqualTo(expected)
        }
    }
}