package mega.privacy.android.domain.usecase.filelink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileLinkRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetFileUrlByPublicLinkUseCaseTest {
    private lateinit var underTest: GetFileUrlByPublicLinkUseCase
    private val repository: FileLinkRepository = mock()

    @Before
    fun setUp() {
        underTest = GetFileUrlByPublicLinkUseCase(repository)
    }

    @Test
    fun `test that valid file url is returned`() = runTest {
        val url = "https://mega.co.nz/abc"
        val localLink = "Local Link"

        whenever(repository.getFileUrlByPublicLink(url)).thenReturn(localLink)
        assertThat(underTest(url)).isEqualTo(localLink)
    }
}