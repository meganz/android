package mega.privacy.android.app.presentation.upload

import android.net.Uri
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadDestinationViewModelTest {


    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getFileForUploadUseCase = mock<GetFileForUploadUseCase>()
    private lateinit var viewModel: UploadDestinationViewModel

    @BeforeEach
    fun setup() {
        reset(getFileForUploadUseCase, getFileForUploadUseCase)
        viewModel = UploadDestinationViewModel(
            getFeatureFlagValueUseCase,
            getFileForUploadUseCase
        )
    }

    @Test
    fun `test that uiState is initialized with default values`() = runTest {
        val uiState = viewModel.uiState.value
        assertTrue(uiState.fileUriList.isEmpty())
        assertTrue(uiState.importUiItems.isEmpty())
    }

    @Test
    fun `test that updateUri updates the list of Uri of the files to upload`() = runTest {
        whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
            .thenReturn(true)
        val fileUriList = listOf(Uri.parse("file1"), Uri.parse("file2"))
        viewModel.updateUri(fileUriList)
        val uiState = viewModel.uiState.value
        assertTrue(uiState.fileUriList == fileUriList)
    }

}