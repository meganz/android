package mega.privacy.android.domain.usecase.login

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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearRestoreCredentialUseCaseTest {
    private lateinit var underTest: ClearRestoreCredentialUseCase

    private val restoreCredentialRepository = mock<RestoreCredentialRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearRestoreCredentialUseCase(
            restoreCredentialRepository = restoreCredentialRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(restoreCredentialRepository)
    }

    @Test
    fun `test that invoke clears the restore credential in the repository`() = runTest {
        underTest()

        verify(restoreCredentialRepository).clearRestoreCredential()
    }

    @Test
    fun `test that invoke throws when repository fails`() = runTest {
        whenever(restoreCredentialRepository.clearRestoreCredential())
            .thenThrow(RestoreCredentialException.Failed())

        assertThrows<RestoreCredentialException.Failed> { underTest() }
    }
}
