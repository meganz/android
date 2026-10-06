package mega.privacy.android.domain.usecase.login

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.CreatedRestoreCredential
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
class CreateRestoreCredentialUseCaseTest {
    private lateinit var underTest: CreateRestoreCredentialUseCase

    private val restoreCredentialRepository = mock<RestoreCredentialRepository>()

    private val requestJson = """{"request":true}"""

    @BeforeAll
    fun setUp() {
        underTest = CreateRestoreCredentialUseCase(
            restoreCredentialRepository = restoreCredentialRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(restoreCredentialRepository)
    }

    @Test
    fun `test that invoke returns the created credential from repository`() = runTest {
        val expected = CreatedRestoreCredential(
            responseJson = """{"response":true}""",
            isBackedUpToCloud = false,
        )
        whenever(restoreCredentialRepository.createRestoreCredential(requestJson))
            .thenReturn(expected)

        val actual = underTest(requestJson)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke throws when repository rejects the request`() = runTest {
        whenever(restoreCredentialRepository.createRestoreCredential(requestJson))
            .thenThrow(RestoreCredentialException.InvalidRequest())

        assertThrows<RestoreCredentialException.InvalidRequest> { underTest(requestJson) }
    }
}
