package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.camerauploads.RemoveBackupFolderUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class LogoutUseCaseTest {
    private lateinit var underTest: LogoutUseCase

    private val loginRepository = mock<LoginRepository>()
    private val removeBackupFolderUseCase = mock<RemoveBackupFolderUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = LogoutUseCase(
            loginRepository = loginRepository,
            removeBackupFolderUseCase = removeBackupFolderUseCase,
        )
    }

    @Test
    internal fun `test that logout flag is set to true`() = runTest {
        underTest()
        verify(loginRepository).setLogoutInProgressFlag(true)
    }

    @Test
    internal fun `test that primary and secondary backup folders are removed`() = runTest {
        underTest()
        verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Primary)
        verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Secondary)
    }

    @Test
    internal fun `test that logout is called`() = runTest {
        underTest()
        verify(loginRepository).logout()
    }

    @Test
    internal fun `test that logout flag is set to false if exception is thrown`() = runTest {
        loginRepository.stub {
            onBlocking { logout() }.thenAnswer { throw Exception("Logout failed") }
        }

        assertThrows<Exception> { underTest() }

        verify(loginRepository).setLogoutInProgressFlag(false)
    }
}