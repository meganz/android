package test.mega.privacy.android.app.uploadFolder

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.uploadFolder.UploadFolderViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import kotlin.test.Test

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadFolderViewModelTest {
    private lateinit var underTest: UploadFolderViewModel

    private val checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = UploadFolderViewModel(
            getFilesInDocumentFolderUseCase = mock(),
            applySortOrderToDocumentFolderUseCase = mock(),
            documentEntityDataMapper = mock(),
            searchFilesInDocumentFolderRecursiveUseCase = mock(),
            checkFileNameCollisionsUseCase = checkFileNameCollisionsUseCase
        )
    }

    @BeforeEach
    fun reset() {
        reset(checkFileNameCollisionsUseCase)
    }

    @Test
    fun `test that the state event is triggered when proceedWithUpload is invoked`() =
        runTest {
            underTest.consumeTransferTriggerEvent()
            underTest.proceedWithUpload(null)

            val actual = underTest.uiState.value.transferTriggerEvent

            // Just check the class, not the content for now, it can be updated when the view model is refactored to be more testable
            assertThat(actual).isInstanceOf(StateEventWithContentTriggered::class.java)
        }

    @Test
    fun `test that the state event is consumed when consume transfer trigger event is invoked`() =
        runTest {
            underTest.proceedWithUpload(null)
            underTest.consumeTransferTriggerEvent()

            val actual = underTest.uiState.value.transferTriggerEvent

            assertThat(actual).isInstanceOf(StateEventWithContentConsumed::class.java)
        }
}