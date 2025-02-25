package mega.privacy.android.app.presentation.filestorage

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.file.FileStorageType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.GetFileStorageTypeNameUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileStorageViewModelTest {

    private lateinit var underTest: FileStorageViewModel
    private val getFileStorageTypeNameUseCase = mock<GetFileStorageTypeNameUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = FileStorageViewModel(getFileStorageTypeNameUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(getFileStorageTypeNameUseCase)
    }

    @Test
    fun `test that storageType is updated correctly when file is valid`() = runTest {
        val path = "foo"
        val file = mock<File> {
            on { absolutePath } doReturn path
        }
        val uriPath = UriPath(path)
        val storageType = FileStorageType.SdCard
        whenever(getFileStorageTypeNameUseCase.invoke(uriPath)).thenReturn(storageType)
        underTest.updateTitle(file)
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.storageType).isEqualTo(storageType)
        }
    }

    @Test
    fun `test that storageType is not updated when file is null`() = runTest {
        underTest.updateTitle(null)
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.storageType).isEqualTo(FileStorageType.Unknown)
        }
    }
}