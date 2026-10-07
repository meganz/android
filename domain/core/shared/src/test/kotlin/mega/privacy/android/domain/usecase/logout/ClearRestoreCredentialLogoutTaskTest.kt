package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.login.ClearRestoreCredentialUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class ClearRestoreCredentialLogoutTaskTest {
    private lateinit var underTest: ClearRestoreCredentialLogoutTask

    private val clearRestoreCredentialUseCase = mock<ClearRestoreCredentialUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearRestoreCredentialLogoutTask(
            clearRestoreCredentialUseCase = clearRestoreCredentialUseCase,
        )
    }

    @Test
    internal fun `test that onLogoutSuccess clears the restore credential`() = runTest {
        underTest.onLogoutSuccess()

        verify(clearRestoreCredentialUseCase).invoke()
    }

    @Test
    internal fun `test that onLogoutFailed does not clear the restore credential`() = runTest {
        underTest.onLogoutFailed(RuntimeException())

        verifyNoInteractions(clearRestoreCredentialUseCase)
    }
}
