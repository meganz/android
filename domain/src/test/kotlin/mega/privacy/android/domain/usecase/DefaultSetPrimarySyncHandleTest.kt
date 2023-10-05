package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSyncHandleUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class DefaultSetPrimarySyncHandleTest {
    private lateinit var underTest: SetPrimarySyncHandle

    private val setupCameraUploadsSyncHandleUseCase = mock<SetupCameraUploadsSyncHandleUseCase>()
    private val broadcastFolderDestinationUpdateUseCase =
        mock<BroadcastFolderDestinationUpdateUseCase>()

    @Before
    fun setUp() {
        underTest = DefaultSetPrimarySyncHandle(
            setupCameraUploadsSyncHandleUseCase = setupCameraUploadsSyncHandleUseCase,
            broadcastFolderDestinationUpdateUseCase = broadcastFolderDestinationUpdateUseCase
        )
    }

    @Test
    fun `test that primary sync handle is set and folder icons and destinations are updated if primary sync handle is updated`() =
        runTest {
            val result = 69L
            underTest(result)
            verify(setupCameraUploadsSyncHandleUseCase).invoke(result)
            verify(broadcastFolderDestinationUpdateUseCase).invoke(result, false)
        }
}
