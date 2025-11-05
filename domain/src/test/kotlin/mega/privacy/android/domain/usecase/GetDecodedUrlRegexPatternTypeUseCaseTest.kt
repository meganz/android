package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.DecodeLinkUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDecodedUrlRegexPatternTypeUseCaseTest {
    private lateinit var underTest: GetDecodedUrlRegexPatternTypeUseCase

    private val getUrlRegexPatternTypeUseCase = mock<GetUrlRegexPatternTypeUseCase>()
    private val decodeLinkUseCase = mock<DecodeLinkUseCase>()

    @BeforeEach
    fun setUp() {
        reset(getUrlRegexPatternTypeUseCase, decodeLinkUseCase)
        underTest = GetDecodedUrlRegexPatternTypeUseCase(
            getUrlRegexPatternTypeUseCase = getUrlRegexPatternTypeUseCase,
            decodeLinkUseCase = decodeLinkUseCase
        )
    }

    @Test
    fun `test that null URL returns null`() {
        val result = underTest(null)
        assertThat(result).isNull()
    }

    @Test
    fun `test that URL is decoded and pattern type is checked`() {
        val originalUrl = "mega://file/abc123"
        val decodedUrl = "https://mega.nz/file/abc123"
        val expectedPatternType = RegexPatternType.FILE_LINK

        whenever(decodeLinkUseCase(originalUrl)).thenReturn(decodedUrl)
        whenever(getUrlRegexPatternTypeUseCase(decodedUrl)).thenReturn(expectedPatternType)

        val result = underTest(originalUrl)

        assertThat(result).isEqualTo(expectedPatternType)
    }
}
