package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.BroadcastFolderDestinationUpdateUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetSecondaryNodeIdUseCaseTest {
    private lateinit var underTest: SetSecondaryNodeIdUseCase

    private val setupMediaUploadsSyncHandleUseCase = mock<SetupMediaUploadsSyncHandleUseCase>()
    private val broadcastFolderDestinationUpdateUseCase =
        mock<BroadcastFolderDestinationUpdateUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetSecondaryNodeIdUseCase(
            setupMediaUploadsSyncHandleUseCase = setupMediaUploadsSyncHandleUseCase,
            broadcastFolderDestinationUpdateUseCase = broadcastFolderDestinationUpdateUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setupMediaUploadsSyncHandleUseCase,
            broadcastFolderDestinationUpdateUseCase,
        )
    }

    @Test
    fun `test that secondary sync handle is set and folder icons and destinations are updated if secondary sync handle is updated`() =
        runTest {
            val result = NodeId(69L)
            underTest(result)
            verify(setupMediaUploadsSyncHandleUseCase).invoke(result.longValue)
            verify(broadcastFolderDestinationUpdateUseCase).invoke(result.longValue, true)
        }
}
