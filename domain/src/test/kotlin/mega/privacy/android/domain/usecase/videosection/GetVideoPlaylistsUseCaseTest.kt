package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.videosection.FavouritesVideoPlaylist
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoPlaylistsUseCaseTest {
    private lateinit var underTest: GetVideoPlaylistsUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    private val favouritesVideoPlaylist = mock<FavouritesVideoPlaylist>()

    @BeforeAll
    fun setUp() {
        underTest = GetVideoPlaylistsUseCase(
            getCloudSortOrder = getCloudSortOrder,
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository, getCloudSortOrder)
    }

    @Test
    fun `test that videoPlaylists is not empty`() = runTest {
        initOrder(SortOrder.ORDER_DEFAULT_ASC)
        val list = listOf(mock<UserVideoPlaylist>())
        whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
        assertThat(underTest()).isNotEmpty()
    }

    @ParameterizedTest(name = "when SortOrder is {0}")
    @EnumSource(
        value = SortOrder::class,
        names = ["ORDER_DEFAULT_ASC", "ORDER_DEFAULT_DESC"]
    )
    fun `test that order of returned videoPlaylists is correctly`(
        sortOrder: SortOrder
    ) =
        runTest {
            val playlist1 = initUserVideoPlaylist(title = "c")
            val playlist2 = initUserVideoPlaylist(title = "a")
            val playlist3 = initUserVideoPlaylist(title = "b")

            initOrder(sortOrder)
            val list = listOf(
                favouritesVideoPlaylist,
                playlist1,
                playlist2,
                playlist3
            )
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
            val actual = underTest()
            assertThat(actual).isNotEmpty()
            assertThat((actual[0] is FavouritesVideoPlaylist)).isTrue()
            if (sortOrder == SortOrder.ORDER_DEFAULT_ASC) {
                assertThat((actual[1] as? UserVideoPlaylist)?.title).isEqualTo(playlist2.title)
                assertThat((actual[2] as? UserVideoPlaylist)?.title).isEqualTo(playlist3.title)
                assertThat((actual[3] as? UserVideoPlaylist)?.title).isEqualTo(playlist1.title)
            } else {
                assertThat((actual[1] as? UserVideoPlaylist)?.title).isEqualTo(playlist1.title)
                assertThat((actual[2] as? UserVideoPlaylist)?.title).isEqualTo(playlist3.title)
                assertThat((actual[3] as? UserVideoPlaylist)?.title).isEqualTo(playlist2.title)
            }
        }

    @ParameterizedTest(name = "when SortOrder is {0}")
    @EnumSource(
        value = SortOrder::class,
        names = ["ORDER_LABEL_DESC", "ORDER_LABEL_ASC", "ORDER_FAV_DESC", "ORDER_FAV_ASC", "ORDER_SIZE_DESC", "ORDER_SIZE_ASC"]
    )
    fun `test that order of returned videoPlaylists is correctly based on ORDER_DEFAULT_ASC`(
        sortOrder: SortOrder
    ) =
        runTest {
            val playlist1 = initUserVideoPlaylist(title = "c")
            val playlist2 = initUserVideoPlaylist(title = "a")
            val playlist3 = initUserVideoPlaylist(title = "b")

            initOrder(sortOrder)
            val list = listOf(
                favouritesVideoPlaylist,
                playlist1,
                playlist2,
                playlist3
            )
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
            val actual = underTest()
            assertThat(actual).isNotEmpty()
            assertThat((actual[0] is FavouritesVideoPlaylist)).isTrue()
            assertThat((actual[1] as? UserVideoPlaylist)?.title).isEqualTo(playlist2.title)
            assertThat((actual[2] as? UserVideoPlaylist)?.title).isEqualTo(playlist3.title)
            assertThat((actual[3] as? UserVideoPlaylist)?.title).isEqualTo(playlist1.title)
        }

    @ParameterizedTest(name = "when SortOrder is {0}")
    @EnumSource(
        value = SortOrder::class,
        names = ["ORDER_CREATION_ASC", "ORDER_CREATION_DESC"]
    )
    fun `test that order of returned videoPlaylists is correctly related to creation time`(
        sortOrder: SortOrder
    ) =
        runTest {
            val playlist1 = initUserVideoPlaylist(title = "a", creationTime = 3L)
            val playlist2 = initUserVideoPlaylist(title = "b", creationTime = 1L)
            val playlist3 = initUserVideoPlaylist(title = "c", creationTime = 2L)

            initOrder(sortOrder)
            val list = listOf(
                favouritesVideoPlaylist,
                playlist1,
                playlist2,
                playlist3
            )
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
            val actual = underTest()
            assertThat(actual).isNotEmpty()
            assertThat((actual[0] is FavouritesVideoPlaylist)).isTrue()
            if (sortOrder == SortOrder.ORDER_CREATION_ASC) {
                assertThat((actual[1] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist2.creationTime)
                assertThat((actual[2] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist3.creationTime)
                assertThat((actual[3] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist1.creationTime)
            } else {
                assertThat((actual[1] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist1.creationTime)
                assertThat((actual[2] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist3.creationTime)
                assertThat((actual[3] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist2.creationTime)
            }
        }

    @ParameterizedTest(name = "when SortOrder is {0}")
    @EnumSource(
        value = SortOrder::class,
        names = ["ORDER_MODIFICATION_ASC", "ORDER_MODIFICATION_DESC"]
    )
    fun `test that order of returned videoPlaylists is correctly related to modification time`(
        sortOrder: SortOrder
    ) =
        runTest {
            val playlist1 = initUserVideoPlaylist(title = "a", modificationTime = 3L)
            val playlist2 = initUserVideoPlaylist(title = "b", modificationTime = 1L)
            val playlist3 = initUserVideoPlaylist(title = "c", modificationTime = 2L)

            initOrder(sortOrder)
            val list = listOf(
                favouritesVideoPlaylist,
                playlist1,
                playlist2,
                playlist3
            )
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
            val actual = underTest()
            assertThat(actual).isNotEmpty()
            assertThat((actual[0] is FavouritesVideoPlaylist)).isTrue()
            if (sortOrder == SortOrder.ORDER_MODIFICATION_ASC) {
                assertThat((actual[1] as? UserVideoPlaylist)?.modificationTime).isEqualTo(playlist2.modificationTime)
                assertThat((actual[2] as? UserVideoPlaylist)?.modificationTime).isEqualTo(playlist3.modificationTime)
                assertThat((actual[3] as? UserVideoPlaylist)?.modificationTime).isEqualTo(playlist1.modificationTime)
            } else {
                assertThat((actual[1] as? UserVideoPlaylist)?.modificationTime).isEqualTo(playlist1.modificationTime)
                assertThat((actual[2] as? UserVideoPlaylist)?.modificationTime).isEqualTo(playlist3.modificationTime)
                assertThat((actual[3] as? UserVideoPlaylist)?.modificationTime).isEqualTo(playlist2.modificationTime)
            }
        }

    @Test
    fun `test that videoPlaylists is empty`() =
        runTest {
            initOrder(SortOrder.ORDER_DEFAULT_ASC)
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(emptyList())
            assertThat(underTest()).isEmpty()
        }

    private suspend fun initOrder(order: SortOrder) {
        whenever(getCloudSortOrder()).thenReturn(order)
    }

    private fun initUserVideoPlaylist(
        id: Long = 1L,
        title: String = "",
        numberOfVideos: Int = 0,
        creationTime: Long = 0L,
        modificationTime: Long = 0L
    ) = UserVideoPlaylist(
        id = NodeId(id),
        title = title,
        numberOfVideos = numberOfVideos,
        creationTime = creationTime,
        cover = null,
        modificationTime = modificationTime,
        thumbnailList = null,
        totalDuration = Duration.ZERO,
        videos = null,
    )
}