package mega.privacy.android.domain.usecase.filelink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.LinksRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecryptPasswordProtectedLinkUseCaseTest {
    private lateinit var underTest: DecryptPasswordProtectedLinkUseCase
    private val repository: LinksRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = DecryptPasswordProtectedLinkUseCase(repository)
    }

    @BeforeEach
    fun cleanUp() {
        reset(repository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://mega.co.nz/decryptedLink"])
    @NullSource
    fun `test that decrypted link from repository is returned when password is correct`(
        decryptedLink: String?,
    ) = runTest {
        val passwordProtectedLink = "https://mega.co.nz/#!encryptedLink"
        val password = "password"

        whenever(
            repository.decryptPasswordProtectedLink(
                passwordProtectedLink,
                password
            )
        ).thenReturn(decryptedLink)

        val result = underTest(passwordProtectedLink, password)

        assertThat(result).isEqualTo(decryptedLink)
    }
}

