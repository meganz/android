package mega.privacy.android.feature.sync.domain.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSolvedIssuesBySyncIdUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoveFolderPairUseCaseTest {

    private lateinit var underTest: RemoveFolderPairUseCase
    private val syncRepository = mock<SyncRepository>()
    private val clearSolvedIssuesBySyncIdUseCase = mock<ClearSolvedIssuesBySyncIdUseCase>()
    private val setUserPausedSyncUseCase = mock<SetUserPausedSyncUseCase>()

    private val folderPairId = 123L

    @BeforeAll
    fun setUp() {
        underTest = RemoveFolderPairUseCase(
            syncRepository = syncRepository,
            clearSolvedIssuesBySyncIdUseCase = clearSolvedIssuesBySyncIdUseCase,
            setUserPausedSyncUseCase = setUserPausedSyncUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncRepository, clearSolvedIssuesBySyncIdUseCase, setUserPausedSyncUseCase)
    }

    @Test
    fun `test that invoke removes the sync then clears its solved issues and its user pause`() =
        runTest {
            underTest(folderPairId)

            inOrder(syncRepository, clearSolvedIssuesBySyncIdUseCase, setUserPausedSyncUseCase) {
                verify(syncRepository).removeFolderPair(folderPairId)
                verify(clearSolvedIssuesBySyncIdUseCase).invoke(folderPairId)
                verify(setUserPausedSyncUseCase).invoke(folderPairId, paused = false)
            }
        }

    @Test
    fun `test that invoke keeps the user pause when removing the sync fails`() = runTest {
        whenever(syncRepository.removeFolderPair(any())).thenThrow(RuntimeException())

        val result = runCatching { underTest(folderPairId) }

        assertThat(result.isFailure).isTrue()
        verifyNoInteractions(clearSolvedIssuesBySyncIdUseCase, setUserPausedSyncUseCase)
    }
}
