package mega.privacy.android.data.mapper.settings

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CookieSettingsMapperTest {

    private lateinit var underTest: CookieSettingsMapper

    @BeforeAll
    fun setUp() {
        underTest = CookieSettingsMapper()
    }

    @ParameterizedTest(name = "numDetails: {0}, expected: {1}")
    @MethodSource("provideTestCases")
    fun `test that correct set is returned based on numDetails`(
        numDetails: Int,
        expected: Set<CookieType>,
    ) {
        val result = underTest(numDetails)
        assertThat(result).isEqualTo(expected)
    }

    private fun provideTestCases() = listOf(
        Arguments.of(1, setOf(CookieType.ESSENTIAL)),
        Arguments.of(3, setOf(CookieType.ESSENTIAL, CookieType.PREFERENCE)),
        Arguments.of(
            5, setOf(CookieType.ESSENTIAL, CookieType.ANALYTICS)
        ),
        Arguments.of(
            9, setOf(CookieType.ESSENTIAL, CookieType.ADVERTISEMENT)
        ),
        Arguments.of(
            7, setOf(CookieType.ESSENTIAL, CookieType.PREFERENCE, CookieType.ANALYTICS)
        ),
        Arguments.of(
            11, setOf(CookieType.ESSENTIAL, CookieType.PREFERENCE, CookieType.ADVERTISEMENT)
        ),
        Arguments.of(
            13, setOf(CookieType.ESSENTIAL, CookieType.ANALYTICS, CookieType.ADVERTISEMENT)
        ),
        Arguments.of(
            15,
            setOf(
                CookieType.ESSENTIAL,
                CookieType.ANALYTICS,
                CookieType.ADVERTISEMENT,
                CookieType.PREFERENCE
            )
        ),
    )
}