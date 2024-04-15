package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLocalFolderLinkFromMegaApiFolderUseCaseTest {
    private lateinit var underTest: GetLocalFolderLinkFromMegaApiFolderUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest =
            GetLocalFolderLinkFromMegaApiFolderUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that result is null when the local link is null`() =
        runTest {
            val testHandle = 123456L
            whenever(mediaPlayerRepository.getLocalLinkForFolderLinkFromMegaApiFolder(testHandle))
                .thenReturn(null)
            assertThat(underTest(testHandle)).isNull()
        }

    @Test
    fun `test that the local link is returned`() =
        runTest {
            val testHandle = 123456L
            val testLink = "expected link"
            whenever(mediaPlayerRepository.getLocalLinkForFolderLinkFromMegaApiFolder(testHandle))
                .thenReturn(testLink)
            assertThat(underTest(testHandle)).isEqualTo(testLink)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            val testHandle = 123456L
            underTest(testHandle)
            verify(mediaPlayerRepository).getLocalLinkForFolderLinkFromMegaApiFolder(testHandle)
        }
}