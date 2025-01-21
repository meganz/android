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
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val updateCookieSettingsUseCase = mock<UpdateCookieSettingsUseCase>()

    @AfterEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
        )
    }

    @BeforeAll
    fun initTestClass() {
        underTest = ShouldShowCookieDialogWithAdsUseCase(
            getFeatureFlagValueUseCase,
        )
    }

    private fun provideTestCases() = listOf(
        Arguments.of(true, emptySet<CookieType>(), true),
        Arguments.of(false, emptySet<CookieType>(), false),
        Arguments.of(true, setOf(CookieType.ESSENTIAL), true),
        Arguments.of(true, setOf(CookieType.ESSENTIAL, CookieType.ADS_CHECK), false),
        Arguments.of(true, setOf(CookieType.ADS_CHECK), false),
    )

    @ParameterizedTest(name = "The visibility of cookie dialog with ads should be: {3} when ads are: {0}, external ads are: {1}, and cookie settings is: {2}")
    @MethodSource("provideTestCases")
    fun `test that show cookie dialog with ads should return expected value when all required fields are provided`(
        isExternalAdsEnabledFeature: Boolean,
        cookieSettings: Set<CookieType>,
        expected: Boolean,
    ) {
        runTest {
            whenever(getFeatureFlagValueUseCase.invoke(any())).thenReturn(
                isExternalAdsEnabledFeature
            )
            whenever(updateCookieSettingsUseCase.invoke(any())).thenReturn(Unit)
            val result = underTest.invoke(
                cookieSettings = cookieSettings,
                isExternalAdsEnabledFeature = mock(),
            )

            assertThat(result).isEqualTo(expected)
        }
    }
}