package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLocalFilePathUseCaseTest {
    private lateinit var underTest: GetLocalFilePathUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetLocalFilePathUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that result is null when local file path is null`() =
        runTest {
            whenever(mediaPlayerRepository.getLocalFilePath(anyOrNull())).thenReturn(null)
            assertThat(underTest(null)).isNull()
        }

    @Test
    fun `test that result is null when the parameter is a folder node`() =
        runTest {
            val folderNode = mock<TypedFolderNode>()
            whenever(mediaPlayerRepository.getLocalFilePath(anyOrNull())).thenReturn("")
            assertThat(underTest(folderNode)).isNull()
        }

    @Test
    fun `test that the local file path is returned`() =
        runTest {
            val testLocalFilePath = "local file path"
            val fileNode = mock<TypedFileNode>()
            whenever(mediaPlayerRepository.getLocalFilePath(fileNode)).thenReturn(testLocalFilePath)
            assertThat(underTest(fileNode)).isEqualTo(testLocalFilePath)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            val fileNode = mock<TypedFileNode>()
            underTest(fileNode)
            Mockito.verify(mediaPlayerRepository).getLocalFilePath(fileNode)
        }
}