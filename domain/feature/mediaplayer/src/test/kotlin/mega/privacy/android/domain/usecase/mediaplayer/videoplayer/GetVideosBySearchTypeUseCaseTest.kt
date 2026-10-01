package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideosBySearchTypeUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideosBySearchTypeUseCaseTest {
    private lateinit var underTest: GetVideosBySearchTypeUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val handle = 1234567L
    private val sortOrder = SortOrder.ORDER_DEFAULT_ASC

    @BeforeAll
    fun initialise() {
        underTest = GetVideosBySearchTypeUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaPlayerRepository)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that videos is not empty`() = runTest {
        val list = listOf(mock<TypedVideoNode>())
        whenever(
            mediaPlayerRepository.getVideosBySearchType(
                handle = handle,
                searchString = "*",
                recursive = true,
                order = sortOrder
            )
        ).thenReturn(list)

        Truth.assertThat(
            underTest(
                handle = handle,
                searchString = "*",
                recursive = true,
                order = sortOrder
            )
        ).isNotEmpty()
    }

    @Test
    fun `test that videos is empty`() = runTest {
        whenever(
            mediaPlayerRepository.getVideosBySearchType(
                handle = handle,
                searchString = "*",
                recursive = true,
                order = sortOrder
            )
        ).thenReturn(emptyList())

        Truth.assertThat(
            underTest(
                handle = handle,
                searchString = "*",
                recursive = true,
                order = sortOrder
            )
        ).isEmpty()
    }
}