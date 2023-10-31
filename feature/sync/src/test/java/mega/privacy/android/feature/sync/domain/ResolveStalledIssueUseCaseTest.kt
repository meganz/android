package mega.privacy.android.feature.sync.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.domain.usecase.file.DeleteFileUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResolveStalledIssueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.reset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)

class ResolveStalledIssueUseCaseTest {

    private val deleteFileUseCase: DeleteFileUseCase = mock()
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase = mock()
    private val underTest = ResolveStalledIssueUseCase(deleteFileUseCase, moveNodesToRubbishUseCase)

    @AfterEach
    fun resetAndTearDown() {
        reset(
            deleteFileUseCase,
            moveNodesToRubbishUseCase,
        )
    }

    @Test
    fun `test that moveNodesToRubbishUseCase is invoked when resolutionActionType is CHOOSE_LOCAL_FILE`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose local file",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
            )
            val megaNodeId = 1L
            val localPath = "localPath"

            underTest(stalledIssueResolutionAction, megaNodeId, localPath)

            verify(moveNodesToRubbishUseCase).invoke(listOf(megaNodeId))
            verifyNoMoreInteractions(deleteFileUseCase)
        }

    @Test
    fun `test that removeFileUseCase is invoked when resolutionActionType is CHOOSE_REMOTE_FILE`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose remote file",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE
            )
            val megaNodeId = 1L
            val localPath = "localPath"

            underTest(stalledIssueResolutionAction, megaNodeId, localPath)

            verify(deleteFileUseCase).invoke(localPath)
            verifyNoMoreInteractions(moveNodesToRubbishUseCase)
        }
}
