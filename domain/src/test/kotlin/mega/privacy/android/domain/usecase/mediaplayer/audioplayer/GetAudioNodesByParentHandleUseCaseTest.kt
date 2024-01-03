package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class GetAudioNodesByParentHandleUseCaseTest {
    lateinit var underTest: GetAudioNodesByParentHandleUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val parentHandle = 123L
    private val order = SortOrder.ORDER_MODIFICATION_DESC

    @BeforeAll
    fun setUp() {
        underTest = GetAudioNodesByParentHandleUseCase(
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
        val list = listOf(mock<TypedAudioNode>())
        whenever(mediaPlayerRepository.getAudioNodesByParentHandle(parentHandle, order)).thenReturn(
            list
        )

        assertThat(underTest(parentHandle, order)).isNotEmpty()
    }

    @Test
    fun `test that audios is empty`() = runTest {
        whenever(mediaPlayerRepository.getAudioNodesByParentHandle(parentHandle, order)).thenReturn(
            emptyList()
        )

        assertThat(underTest(parentHandle, order)).isEmpty()
    }
}