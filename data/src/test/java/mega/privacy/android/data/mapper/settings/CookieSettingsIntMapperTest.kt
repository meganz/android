package mega.privacy.android.data.mapper.settings

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CookieSettingsIntMapperTest {

    private lateinit var underTest: CookieSettingsIntMapper

    @BeforeAll
    fun setUp() {
        underTest = CookieSettingsIntMapper()
    }

    @ParameterizedTest(name = "enabled cookies: {0}, expected decimal: {1}")
    @MethodSource("provideTestCases")
    fun `test that correct decimal is returned based on cookie list`(
        enabledCookieSet: Set<CookieType>,
        expectedDecimal: Int,
    ) {
        val result = underTest(enabledCookieSet)
        assertThat(result).isEqualTo(expectedDecimal)
    }

    private fun provideTestCases() = listOf(
        Arguments.of(setOf(CookieType.ESSENTIAL), 1),
        Arguments.of(setOf(CookieType.ESSENTIAL, CookieType.PREFERENCE), 3),
        Arguments.of(setOf(CookieType.ESSENTIAL, CookieType.ANALYTICS), 5),
        Arguments.of(setOf(CookieType.ESSENTIAL, CookieType.ADVERTISEMENT), 9),
        Arguments.of(setOf(CookieType.ESSENTIAL, CookieType.PREFERENCE, CookieType.ANALYTICS), 7),
        Arguments.of(
            setOf(CookieType.ESSENTIAL, CookieType.PREFERENCE, CookieType.ADVERTISEMENT),
            11
        ),
        Arguments.of(
            setOf(CookieType.ESSENTIAL, CookieType.ANALYTICS, CookieType.ADVERTISEMENT),
            13
        ),
        Arguments.of(
            setOf(
                CookieType.ESSENTIAL,
                CookieType.ANALYTICS,
                CookieType.ADVERTISEMENT,
                CookieType.PREFERENCE
            ), 15
        ),
    )
}