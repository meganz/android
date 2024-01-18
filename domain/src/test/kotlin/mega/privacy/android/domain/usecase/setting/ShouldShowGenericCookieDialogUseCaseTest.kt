package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShouldShowGenericCookieDialogUseCaseTest {

    private lateinit var underTest: ShouldShowGenericCookieDialogUseCase
    private val checkCookieBannerEnabledUseCase = mock<CheckCookieBannerEnabledUseCase>()

    @AfterEach
    fun resetMocks() {
        reset(
            checkCookieBannerEnabledUseCase,
        )
    }

    @BeforeAll
    fun initTestClass() {
        underTest = ShouldShowGenericCookieDialogUseCase(
            checkCookieBannerEnabledUseCase,
        )
    }

    private fun provideTestCases() = listOf(
        Arguments.of(true, emptySet<CookieType>(), true),
        Arguments.of(false, emptySet<CookieType>(), false),
        Arguments.of(true, setOf(CookieType.ESSENTIAL), false),
        Arguments.of(false, setOf(CookieType.ESSENTIAL), false)
    )

    @ParameterizedTest(name = "generic cookie dialog's visibility is: {2} when cookie banner is: {0} and cookie settings is: {1}")
    @MethodSource("provideTestCases")
    fun `test that show generic cookie dialog should return expected value when cookie banner and cookie settings are returned`(
        cookieBannerEnabled: Boolean,
        cookieSettings: Set<CookieType>,
        expected: Boolean,
    ) {
        runTest {
            whenever(checkCookieBannerEnabledUseCase()).thenReturn(cookieBannerEnabled)

            val result = underTest.invoke(cookieSettings)
            assertThat(result).isEqualTo(expected)
        }
    }
}