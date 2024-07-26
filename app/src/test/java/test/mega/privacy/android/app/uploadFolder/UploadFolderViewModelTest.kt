package test.mega.privacy.android.app.uploadFolder

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.uploadFolder.UploadFolderViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadFolderViewModelTest {
    private lateinit var underTest: UploadFolderViewModel

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = UploadFolderViewModel(
            getFolderContentUseCase = mock(),
            getFilesInDocumentFolderUseCase = mock(),
            applySortOrderToDocumentFolderUseCase = mock(),
            transfersManagement = mock(),
            documentEntityDataMapper = mock(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            searchFilesInDocumentFolderRecursiveUseCase = mock(),
            checkFileNameCollisionsUseCase = checkFileNameCollisionsUseCase
        )
    }

    @BeforeEach
    fun reset() {
        reset(
            getFeatureFlagValueUseCase,
            checkFileNameCollisionsUseCase
        )
    }

    @Test
    fun `test that the state event is triggered when proceedWithUpload is invoked`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.UploadWorker)) doReturn true
            underTest.consumeTransferTriggerEvent()
            underTest.proceedWithUpload(mock(), null)

            val actual = underTest.uiState.value.transferTriggerEvent

            // Just check the class, not the content for now, it can be updated when the view model is refactored to be more testable
            assertThat(actual).isInstanceOf(StateEventWithContentTriggered::class.java)
        }

    @Test
    fun `test that the state event is consumed when consume transfer trigger event is invoked`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.UploadWorker)) doReturn true
            underTest.proceedWithUpload(mock(), null)
            underTest.consumeTransferTriggerEvent()

            val actual = underTest.uiState.value.transferTriggerEvent

            assertThat(actual).isInstanceOf(StateEventWithContentConsumed::class.java)
        }
}