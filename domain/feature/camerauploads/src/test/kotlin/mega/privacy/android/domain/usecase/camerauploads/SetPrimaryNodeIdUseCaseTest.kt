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
class SetPrimaryNodeIdUseCaseTest {
    private lateinit var underTest: SetPrimaryNodeIdUseCase

    private val setupCameraUploadsSyncHandleUseCase =
        mock<SetupCameraUploadsSyncHandleUseCase>()
    private val broadcastFolderDestinationUpdateUseCase =
        mock<BroadcastFolderDestinationUpdateUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetPrimaryNodeIdUseCase(
            setupCameraUploadsSyncHandleUseCase = setupCameraUploadsSyncHandleUseCase,
            broadcastFolderDestinationUpdateUseCase = broadcastFolderDestinationUpdateUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setupCameraUploadsSyncHandleUseCase,
            broadcastFolderDestinationUpdateUseCase,
        )
    }

    @Test
    fun `test that primary sync handle is set and folder icons and destinations are updated if primary sync handle is updated`() =
        runTest {
            val result = NodeId(69L)
            underTest(result)
            verify(setupCameraUploadsSyncHandleUseCase).invoke(result.longValue)
            verify(broadcastFolderDestinationUpdateUseCase).invoke(result.longValue, false)
        }
}
