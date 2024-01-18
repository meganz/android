package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.NullSessionTransferURLException
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSessionTransferURLUseCaseTest {

    private lateinit var underTest: GetSessionTransferURLUseCase

    private val loginRepository = mock<LoginRepository>()

    private val path = "testPath"

    @BeforeAll
    fun setUp() {
        underTest = GetSessionTransferURLUseCase(
            repository = loginRepository,
        )
    }

    @Test
    fun `test that GetSessionTransferURLUseCase throws an exception if URL is null`() =
        runTest {
            whenever(loginRepository.getSessionTransferURL(path)).thenReturn(null)

            assertThrows<NullSessionTransferURLException> { underTest.invoke(path) }
        }
}