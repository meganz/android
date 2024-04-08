package mega.privacy.android.app.camera

import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.usecase.file.DeleteFileByUriUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PreviewViewModelTest {
    private lateinit var underTest: PreviewViewModel
    private val deleteFileByUriUseCase = mock<DeleteFileByUriUseCase>()
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val applicationScope: CoroutineScope = CoroutineScope(testDispatcher)

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        initTestClass()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(deleteFileByUriUseCase)
    }

    @Test
    fun `test that deleting video invokes contentResolver delete`() = runTest {
        val uri = mock<Uri> {
            on { toString() } doReturn "content://mega.privacy.android.app.provider/cache/123456"
        }
        underTest.deleteVideo(uri)
        verify(deleteFileByUriUseCase).invoke(uri.toString())
    }

    private fun initTestClass() {
        underTest = PreviewViewModel(
            applicationScope = applicationScope,
            deleteFileByUriUseCase = deleteFileByUriUseCase
        )
    }
}