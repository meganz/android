package mega.privacy.android.feature.sync.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSyncSolvedIssuesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class ClearSyncSolvedIssuesLogoutTaskTest {
    private lateinit var underTest: ClearSyncSolvedIssuesLogoutTask

    private val clearSyncSolvedIssuesUseCase: ClearSyncSolvedIssuesUseCase = mock()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearSyncSolvedIssuesLogoutTask(
            clearSyncSolvedIssuesUseCase = clearSyncSolvedIssuesUseCase,
        )
    }

    @Test
    internal fun `test that clear sync solved issues use case is called when logout success`() =
        runTest {
            underTest.onLogoutSuccess()
            verify(clearSyncSolvedIssuesUseCase).invoke()
        }
}