package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAudioNodeByHandleUseCaseTest {
    lateinit var underTest: GetAudioNodeByHandleUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetAudioNodeByHandleUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that getAudioNodeByHandle function is invoked and returns the value as expected`() =
        runTest {
            val handle = 123L
            val expectedNode = mock<TypedAudioNode>()
            whenever(mediaPlayerRepository.getAudioNodeByHandle(handle)).thenReturn(expectedNode)
            assertThat(underTest(handle)).isEqualTo(expectedNode)
        }
}