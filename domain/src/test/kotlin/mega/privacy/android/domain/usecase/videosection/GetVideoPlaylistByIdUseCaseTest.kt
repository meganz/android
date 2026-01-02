package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.videosection.FavouritesVideoPlaylist
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoPlaylistByIdUseCaseTest {
    private lateinit var underTest: GetVideoPlaylistByIdUseCase

    private val videoSectionRepository = mock<VideoSectionRepository>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    @BeforeAll
    fun setUp() {
        underTest = GetVideoPlaylistByIdUseCase(
            getCloudSortOrder = getCloudSortOrder,
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            getCloudSortOrder,
            videoSectionRepository
        )
    }

    @Test
    fun `test that return expected value when type is Favourite`() = runTest {
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC
        val favouritePlaylist = mock<FavouritesVideoPlaylist>()
        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(videoSectionRepository.getFavouritePlaylist(sortOrder)).thenReturn(
            favouritePlaylist
        )
        val result = underTest(NodeId(-1), PlaylistType.Favourite)
        assertThat(result).isEqualTo(favouritePlaylist)
    }

    @Test
    fun `test that return expected value when type is User`() = runTest {
        val id = NodeId(123456L)
        val userPlaylist = mock<UserVideoPlaylist>()
        whenever(videoSectionRepository.getVideoPlaylistById(id)).thenReturn(userPlaylist)
        val result = underTest(id, PlaylistType.User)
        assertThat(result).isEqualTo(userPlaylist)
    }
}