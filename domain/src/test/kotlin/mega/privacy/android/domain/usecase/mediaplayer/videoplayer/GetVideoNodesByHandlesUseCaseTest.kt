package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoNodesByHandlesUseCaseTest {
    lateinit var underTest: GetVideoNodesByHandlesUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val handles = listOf(123456L)
    private val expectedHandle = 100L

    @BeforeAll
    fun setUp() {
        underTest = GetVideoNodesByHandlesUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that videos is not empty`() = runTest {
        val videoNode = mock<TypedVideoNode> {
            on { type } doReturn mock<VideoFileTypeInfo>()
        }
        val list = listOf(videoNode)
        whenever(mediaPlayerRepository.getVideoNodesByHandles(handles)).thenReturn(list)

        assertThat(underTest(handles)).isNotEmpty()
    }

    @Test
    fun `test that videos is empty`() = runTest {
        whenever(mediaPlayerRepository.getVideoNodesByHandles(handles)).thenReturn(emptyList())

        assertThat(underTest(handles)).isEmpty()
    }

    @Test
    fun `test that getVideoNodesByHandles returns list of video nodes`() = runTest {
        val handles = listOf(expectedHandle, 200L)
        val videoNode = mock<TypedVideoNode> {
            on { id } doReturn NodeId(expectedHandle)
            on { type } doReturn mock<VideoFileTypeInfo>()
        }
        val audioNode = mock<TypedVideoNode> {
            on { id } doReturn NodeId(200L)
            on { type } doReturn mock<AudioFileTypeInfo>()
        }

        whenever(mediaPlayerRepository.getVideoNodesByHandles(handles)).thenReturn(
            listOf(videoNode, audioNode)
        )

        val actual = underTest(handles)

        assertThat(actual).hasSize(1)
        assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
    }
}