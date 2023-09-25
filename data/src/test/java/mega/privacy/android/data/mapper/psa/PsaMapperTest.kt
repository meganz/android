package mega.privacy.android.data.mapper.psa

import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class PsaMapperTest {
    private lateinit var underTest: PsaMapper

    @BeforeEach
    internal fun setUp() {
        underTest = PsaMapper()
    }

    @Test
    fun `test that values are set from the request`() {
        val expectedNumber = 12
        val expectedName = "expectedName"
        val expectedText = "expectedText"
        val expectedFile = "expectedFile"
        val expectedPassword = "expectedPassword"
        val expectedLink = "expectedLink"
        val expectedEmail = "expectedEmail"

        val request = mock<MegaRequest> {
            on { number }.thenReturn(expectedNumber.toLong())
            on { name }.thenReturn(expectedName)
            on { text }.thenReturn(expectedText)
            on { file }.thenReturn(expectedFile)
            on { password }.thenReturn(expectedPassword)
            on { link }.thenReturn(expectedLink)
            on { email }.thenReturn(expectedEmail)
        }

        val actual = underTest(request)

        assertThat(actual.id).isEqualTo(expectedNumber)
        assertThat(actual.title).isEqualTo(expectedName)
        assertThat(actual.text).isEqualTo(expectedText)
        assertThat(actual.imageUrl).isEqualTo(expectedFile)
        assertThat(actual.positiveText).isEqualTo(expectedPassword)
        assertThat(actual.positiveLink).isEqualTo(expectedLink)
        assertThat(actual.url).isEqualTo(expectedEmail)
    }
}