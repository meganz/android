package mega.privacy.android.domain.usecase.link

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecodeLinkUseCaseTest {
    private val getDomainNameUseCase: GetDomainNameUseCase = mock()
    private lateinit var undertest: DecodeLinkUseCase

    @BeforeEach
    fun setUp() {
        whenever(getDomainNameUseCase()) doReturn "mega.nz"
        undertest = DecodeLinkUseCase(getDomainNameUseCase)
    }

    @ParameterizedTest
    @MethodSource("domainProvider")
    fun `test mega protocol link is converted to https with domain`(domain: String) {
        whenever(getDomainNameUseCase()) doReturn domain
        val input = "mega://folder/abc"
        val expected = "https://$domain/folder/abc"
        val result = undertest(input)
        assertThat(expected).isEqualTo(result)
    }

    @Test
    fun `test mega dot link is converted to https`() {
        val input = "mega.co.nz/folder/abc"
        val expected = "https://mega.co.nz/folder/abc"
        val result = undertest(input)
        assertThat(expected).isEqualTo(result)
    }

    @Test
    fun `test www mega co nz link is converted to mega co nz`() {
        val input = "https://www.mega.co.nz/folder/abc"
        val expected = "https://mega.co.nz/folder/abc"
        val result = undertest(input)
        assertThat(expected).isEqualTo(result)
    }

    @ParameterizedTest
    @MethodSource("domainProvider")
    fun `test www mega nz link is converted to domain`(domain: String) {
        whenever(getDomainNameUseCase()) doReturn domain
        val input = "https://www.mega.nz/folder/abc"
        val expected = "https://$domain/folder/abc"
        val result = undertest(input)
        assertThat(expected).isEqualTo(result)
    }

    @ParameterizedTest
    @MethodSource("domainProvider")
    fun `test www mega app link is converted to domain`(domain: String) {
        whenever(getDomainNameUseCase()) doReturn domain
        val input = "https://www.mega.app/folder/abc"
        val expected = "https://$domain/folder/abc"
        val result = undertest(input)
        assertThat(expected).isEqualTo(result)
    }

    @Test
    fun `test url decoding and space replacement`() {
        val input = "https%3A%2F%2Fmega.nz%2Ffolder%2Fabc%20def"
        val expected = "https://mega.nz/folder/abc+def"
        val result = undertest(input)
        assertThat(expected).isEqualTo(result)
    }

    private fun domainProvider() = listOf("mega.nz", "mega.app")
}