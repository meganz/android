package mega.privacy.android.domain.usecase.link

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSessionLinkUseCaseTest {

    private lateinit var underTest: GetSessionLinkUseCase

    private val getSessionTransferURLUseCase = mock<GetSessionTransferURLUseCase>()

    @BeforeAll
    fun setup() {
        underTest = GetSessionLinkUseCase(getSessionTransferURLUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getSessionTransferURLUseCase)
    }

    @Test
    fun `test that if link does not contain session string it returns null`() = runTest {
        val link = "link"

        assertThat(underTest(link)).isNull()
    }

    @Test
    fun `test that if link contains session string and getSessionTransferURLUseCase fails it returns null`() =
        runTest {
            val link = "fm/link"

            whenever(getSessionTransferURLUseCase(link)) doThrow RuntimeException()

            assertThat(underTest(link)).isNull()
        }

    @Test
    fun `test that if link contains session string and getSessionTransferURLUseCase returns a link, it returns correctly`() =
        runTest {
            val path = "link"
            val link = "fm/$path"
            val expected = "link/with/session"

            whenever(getSessionTransferURLUseCase(path)) doReturn expected

            assertThat(underTest(link)).isEqualTo(expected)
        }
}