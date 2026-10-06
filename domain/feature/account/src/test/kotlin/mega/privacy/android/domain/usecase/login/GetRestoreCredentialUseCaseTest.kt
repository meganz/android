package mega.privacy.android.domain.usecase.login

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.login.RestoreCredentialException
import mega.privacy.android.domain.repository.security.RestoreCredentialRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetRestoreCredentialUseCaseTest {
    private lateinit var underTest: GetRestoreCredentialUseCase

    private val restoreCredentialRepository = mock<RestoreCredentialRepository>()

    private val requestJson = """{"request":true}"""

    @BeforeAll
    fun setUp() {
        underTest = GetRestoreCredentialUseCase(
            restoreCredentialRepository = restoreCredentialRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(restoreCredentialRepository)
    }

    @Test
    fun `test that invoke returns the authentication response from repository`() = runTest {
        val expected = """{"response":true}"""
        whenever(restoreCredentialRepository.getRestoreCredential(requestJson))
            .thenReturn(expected)

        val actual = underTest(requestJson)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke throws when no restore credential exists`() = runTest {
        whenever(restoreCredentialRepository.getRestoreCredential(requestJson))
            .thenThrow(RestoreCredentialException.NoCredential())

        assertThrows<RestoreCredentialException.NoCredential> { underTest(requestJson) }
    }
}
