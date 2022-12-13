package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ImageRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetPreviewTest {
    private lateinit var underTest: GetPreview

    private val imageRepository = mock<ImageRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetPreview(repository = imageRepository)
    }

    @Test
    fun `test that local preview is returned if present`() = runTest {
        val localFile = File("localFile")
        imageRepository.stub {
            onBlocking { getPreviewFromLocal(any()) }.thenReturn(localFile)
        }
        assertThat(underTest(1L)).isSameInstanceAs(localFile)
    }

    @Test
    fun `test that preview is fetched from  the remote if no local file is returned`() = runTest {
        val remoteFile = File("localFile")
        imageRepository.stub {
            onBlocking { getPreviewFromLocal(any()) }.thenReturn(null)
            onBlocking { getPreviewFromServer(any()) }.thenReturn(remoteFile)
        }
        assertThat(underTest(1L)).isSameInstanceAs(remoteFile)
    }
}