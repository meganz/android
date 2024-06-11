package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoPlaylistSetsUseCaseTest {
    private lateinit var underTest: GetVideoPlaylistSetsUseCase

    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetVideoPlaylistSetsUseCase(videoSectionRepository = videoSectionRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository)
    }

    @Test
    fun `test that result is not empty`() = runTest {
        val list = listOf(mock<UserSet>())
        whenever(videoSectionRepository.getVideoPlaylistSets()).thenReturn(list)
        assertThat(underTest()).isNotEmpty()
    }

    @Test
    fun `test that result is empty`() = runTest {
        whenever(videoSectionRepository.getVideoPlaylistSets()).thenReturn(emptyList())
        assertThat(underTest()).isEmpty()
    }

    @Test
    fun `test that the function is invoked as expected`() = runTest {
        underTest()
        verify(videoSectionRepository).getVideoPlaylistSets()
    }
}