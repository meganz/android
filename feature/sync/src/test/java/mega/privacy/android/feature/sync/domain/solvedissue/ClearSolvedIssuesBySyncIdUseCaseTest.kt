package mega.privacy.android.feature.sync.domain.solvedissue

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSolvedIssuesBySyncIdUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearSolvedIssuesBySyncIdUseCaseTest {

    private val syncSolvedIssuesRepository: SyncSolvedIssuesRepository = mock()
    private val underTest = ClearSolvedIssuesBySyncIdUseCase(syncSolvedIssuesRepository)

    @Test
    fun `test that clear solved issues by sync id invokes repository`() = runTest {
        val syncId = 3232323L
        underTest(syncId)

        verify(syncSolvedIssuesRepository).removeBySyncId(syncId)
    }
}