package mega.privacy.android.domain.usecase.thumbnailpreview

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GetPreviewUseCaseTest {
    private lateinit var underTest: GetPreviewUseCase

    private val thumbnailPreviewRepository = mock<ThumbnailPreviewRepository>()

    @Before
    fun setUp() {
        underTest = GetPreviewUseCase(thumbnailPreviewRepository = thumbnailPreviewRepository)
    }

    @Test
    fun `test that local preview is returned if present`() = runTest {
        val localFile = File("localFile")
        thumbnailPreviewRepository.stub {
            onBlocking { getPreviewFromLocal(any()) }.thenReturn(localFile)
        }
        assertThat(underTest(mock())).isSameInstanceAs(localFile)
    }

    @Test
    fun `test that preview is fetched from  the remote if no local file is returned`() = runTest {
        val remoteFile = File("localFile")
        thumbnailPreviewRepository.stub {
            onBlocking { getPreviewFromLocal(any()) }.thenReturn(null)
            onBlocking { getPreviewFromServer(any()) }.thenReturn(remoteFile)
        }
        assertThat(underTest(mock())).isSameInstanceAs(remoteFile)
    }
}