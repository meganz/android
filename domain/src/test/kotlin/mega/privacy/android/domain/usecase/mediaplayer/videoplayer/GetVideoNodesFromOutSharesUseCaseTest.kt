package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromOutSharesUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoNodesFromOutSharesUseCaseTest {
    lateinit var underTest: GetVideoNodesFromOutSharesUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val lastHandle = 123L
    private val order = SortOrder.ORDER_MODIFICATION_DESC

    @BeforeAll
    fun setUp() {
        underTest = GetVideoNodesFromOutSharesUseCase(
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
        val list = listOf(mock<TypedVideoNode>())
        whenever(mediaPlayerRepository.getVideoNodesFromOutShares(lastHandle, order)).thenReturn(
            list
        )

        assertThat(underTest(lastHandle, order)).isNotEmpty()
    }

    @Test
    fun `test that videos is empty`() = runTest {
        whenever(mediaPlayerRepository.getVideoNodesFromOutShares(lastHandle, order)).thenReturn(
            emptyList()
        )

        assertThat(underTest(lastHandle, order)).isEmpty()
    }
}