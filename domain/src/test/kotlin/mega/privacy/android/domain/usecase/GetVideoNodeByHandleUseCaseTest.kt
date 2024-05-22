package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodeByHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoNodeByHandleUseCaseTest {
    private lateinit var underTest: GetVideoNodeByHandleUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()
    private val testHandle = 1L

    @BeforeAll
    fun setUp() {
        underTest = GetVideoNodeByHandleUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that result is null`() =
        runTest {
            whenever(mediaPlayerRepository.getVideoNodeByHandle(testHandle)).thenReturn(null)
            assertThat(underTest(testHandle)).isNull()
        }

    @Test
    fun `test that the video node is returned`() =
        runTest {
            val testVideoNode = mock<TypedVideoNode>()
            whenever(mediaPlayerRepository.getVideoNodeByHandle(testHandle)).thenReturn(
                testVideoNode
            )
            assertThat(underTest(testHandle)).isEqualTo(testVideoNode)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest(testHandle)
            verify(mediaPlayerRepository).getVideoNodeByHandle(testHandle)
        }
}