package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
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
class GetAudioNodesByHandlesUseCaseTest {
    lateinit var underTest: GetAudioNodesByHandlesUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val handles = listOf(123456L)
    private val expectedHandle = 100L

    @BeforeAll
    fun setUp() {
        underTest = GetAudioNodesByHandlesUseCase(
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
    fun `test that audios is not empty`() = runTest {
        val handles = listOf(123456L)
        val audioNode = mock<TypedAudioNode> {
            on { type } doReturn mock<AudioFileTypeInfo>()
        }
        val list = listOf(audioNode)
        whenever(mediaPlayerRepository.getAudioNodesByHandles(handles)).thenReturn(list)

        assertThat(underTest(handles)).isNotEmpty()
    }

    @Test
    fun `test that audios is empty`() = runTest {
        whenever(mediaPlayerRepository.getAudioNodesByHandles(handles)).thenReturn(emptyList())

        assertThat(underTest(handles)).isEmpty()
    }

    @Test
    fun `test that getAudioNodesByHandles returns list of audio nodes`() = runTest {
        val handles = listOf(expectedHandle, 200L)
        val audioNode = mock<TypedAudioNode> {
            on { id } doReturn NodeId(expectedHandle)
            on { type } doReturn mock<AudioFileTypeInfo>()
        }
        val videoNode = mock<TypedAudioNode> {
            on { id } doReturn NodeId(200L)
            on { type } doReturn mock<VideoFileTypeInfo>()
        }

        whenever(mediaPlayerRepository.getAudioNodesByHandles(handles)).thenReturn(
            listOf(audioNode, videoNode)
        )

        val actual = underTest(handles)

        assertThat(actual).hasSize(1)
        assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
    }
}