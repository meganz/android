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
        whenever(videoSectionRepository.getVideoPlaylistSets()).thenReturn(emptyList())

        underTest()
        verify(videoSectionRepository).getVideoPlaylistSets()
    }

    @Test
    fun `test that the sets are ordered by name`() = runTest {
        val sets = listOf(initUserSet(name = "c"), initUserSet(name = "a"), initUserSet(name = "b"))
        whenever(videoSectionRepository.getVideoPlaylistSets()).thenReturn(sets)

        assertThat(underTest().map { it.name }).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun `test that a trailing counter in the name is ordered by value`() = runTest {
        val sets = listOf(
            initUserSet(name = "New playlist (10)"),
            initUserSet(name = "New playlist (2)"),
            initUserSet(name = "New playlist"),
        )
        whenever(videoSectionRepository.getVideoPlaylistSets()).thenReturn(sets)

        assertThat(underTest().map { it.name }).containsExactly(
            "New playlist",
            "New playlist (2)",
            "New playlist (10)",
        ).inOrder()
    }

    private fun initUserSet(name: String) = mock<UserSet> {
        on { this.name }.thenReturn(name)
    }
}
