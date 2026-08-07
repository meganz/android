package mega.privacy.android.domain.usecase.media

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorMediaSystemAlbumsUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorMediaSystemAlbumsUseCaseTest {
    private lateinit var underTest: MonitorMediaSystemAlbumsUseCase
    private val testDispatcher = UnconfinedTestDispatcher()

    private val photosRepository: PhotosRepository = mock()
    private val listMediaNodesByOffsetUseCase: ListMediaNodesByOffsetUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    private val gifAlbum = createMockSystemAlbum(
        albumNameResId = 1,
        hideWhenEmpty = true,
        filter = mediaFilter(subCategory = MediaTimelineFilter.SubCategory.Gif),
    )
    private val rawAlbum = createMockSystemAlbum(
        albumNameResId = 2,
        hideWhenEmpty = true,
        filter = mediaFilter(subCategory = MediaTimelineFilter.SubCategory.Raw),
    )
    private val favouriteAlbum = createMockSystemAlbum(
        albumNameResId = 3,
        hideWhenEmpty = false,
        filter = mediaFilter(favourites = MediaTimelineFilter.Favourites.Favourites),
    )

    @BeforeAll
    fun init() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun setUp() {
        reset(
            photosRepository,
            listMediaNodesByOffsetUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase,
        )
    }

    private fun initUseCase(systemAlbums: Set<SystemAlbum>) {
        underTest = MonitorMediaSystemAlbumsUseCase(
            photosRepository = photosRepository,
            systemAlbums = systemAlbums,
            listMediaNodesByOffsetUseCase = listMediaNodesByOffsetUseCase,
            defaultDispatcher = testDispatcher,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        )
    }

    private fun stubTriggers(showHiddenItems: Boolean, isPaid: Boolean) {
        whenever(photosRepository.monitorMediaTypedNodes).thenReturn(flowOf(emptyList<TypedNode>()))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(showHiddenItems))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid)))
    }

    private suspend fun stubCover(
        album: SystemAlbum,
        sensitivity: MediaTimelineFilter.Sensitivity,
        cover: TypedFileNode?,
    ) {
        whenever(
            listMediaNodesByOffsetUseCase(
                filter = album.mediaTimelineFilter.copy(sensitivity = sensitivity),
                section = null,
                order = SortOrder.ORDER_MODIFICATION_DESC,
                maxElements = 1,
                offset = 0,
            )
        ).thenReturn(cover?.let { listOf(it) } ?: emptyList())
    }

    @Test
    fun `test that each system album is returned with its cover node`() = runTest {
        val gifCover = mock<TypedFileNode>()
        val rawCover = mock<TypedFileNode>()
        val favouriteCover = mock<TypedFileNode>()

        stubTriggers(showHiddenItems = true, isPaid = true)
        stubCover(gifAlbum, MediaTimelineFilter.Sensitivity.ShowAll, gifCover)
        stubCover(rawAlbum, MediaTimelineFilter.Sensitivity.ShowAll, rawCover)
        stubCover(favouriteAlbum, MediaTimelineFilter.Sensitivity.ShowAll, favouriteCover)

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            val result = awaitItem()

            assertThat(result).hasSize(3)
            assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[0].cover).isEqualTo(gifCover)
            assertThat(result[1].cover).isEqualTo(rawCover)
            assertThat(result[2].cover).isEqualTo(favouriteCover)

            awaitComplete()
        }
    }

    @Test
    fun `test that album is omitted when hideWhenEmpty is true and cover is null`() = runTest {
        stubTriggers(showHiddenItems = true, isPaid = true)
        stubCover(gifAlbum, MediaTimelineFilter.Sensitivity.ShowAll, null)

        initUseCase(setOf(gifAlbum))

        underTest().test {
            val result = awaitItem()

            assertThat(result).isEmpty()

            awaitComplete()
        }
    }

    @Test
    fun `test that album is included with null cover when hideWhenEmpty is false and cover is null`() =
        runTest {
            stubTriggers(showHiddenItems = true, isPaid = true)
            stubCover(favouriteAlbum, MediaTimelineFilter.Sensitivity.ShowAll, null)

            initUseCase(setOf(favouriteAlbum))

            underTest().test {
                val result = awaitItem()

                assertThat(result).hasSize(1)
                assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
                assertThat(result[0].cover).isNull()

                awaitComplete()
            }
        }

    @Test
    fun `test that paid account with hidden items disabled applies HideSensitive to the filter`() =
        runTest {
            val cover = mock<TypedFileNode>()
            stubTriggers(showHiddenItems = false, isPaid = true)
            stubCover(gifAlbum, MediaTimelineFilter.Sensitivity.HideSensitive, cover)

            initUseCase(setOf(gifAlbum))

            underTest().test {
                val result = awaitItem()

                assertThat(result).hasSize(1)
                assertThat(result[0].cover).isEqualTo(cover)

                awaitComplete()
            }
        }

    @Test
    fun `test that paid account with hidden items enabled applies ShowAll to the filter`() = runTest {
        val cover = mock<TypedFileNode>()
        stubTriggers(showHiddenItems = true, isPaid = true)
        stubCover(gifAlbum, MediaTimelineFilter.Sensitivity.ShowAll, cover)

        initUseCase(setOf(gifAlbum))

        underTest().test {
            val result = awaitItem()

            assertThat(result).hasSize(1)
            assertThat(result[0].cover).isEqualTo(cover)

            awaitComplete()
        }
    }

    @Test
    fun `test that free account applies ShowAll to the filter`() = runTest {
        val cover = mock<TypedFileNode>()
        stubTriggers(showHiddenItems = false, isPaid = false)
        stubCover(gifAlbum, MediaTimelineFilter.Sensitivity.ShowAll, cover)

        initUseCase(setOf(gifAlbum))

        underTest().test {
            val result = awaitItem()

            assertThat(result).hasSize(1)
            assertThat(result[0].cover).isEqualTo(cover)

            awaitComplete()
        }
    }

    private fun createMockSystemAlbum(
        albumNameResId: Int,
        hideWhenEmpty: Boolean,
        filter: MediaTimelineFilter,
    ): SystemAlbum = object : SystemAlbum {
        override val albumNameResId = albumNameResId
        override val hideWhenEmpty: Boolean = hideWhenEmpty
        override val mediaTimelineFilter: MediaTimelineFilter = filter
    }

    private fun mediaFilter(
        subCategory: MediaTimelineFilter.SubCategory = MediaTimelineFilter.SubCategory.All,
        favourites: MediaTimelineFilter.Favourites = MediaTimelineFilter.Favourites.All,
    ): MediaTimelineFilter = MediaTimelineFilter(
        granularity = MediaTimelineFilter.Granularity.Day,
        category = MediaTimelineFilter.Category.All,
        location = MediaTimelineFilter.Location.CloudDriveAndVault,
        sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
        subCategory = subCategory,
        favourites = favourites,
    )

    private fun createMockAccountDetail(isPaid: Boolean): AccountDetail {
        val accountType = if (isPaid) AccountType.PRO_I else AccountType.FREE
        val accountLevelDetail = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = null,
            subscriptionRenewTime = 0L,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            proExpirationTime = 0L,
            accountPlanDetail = null,
            accountSubscriptionDetailList = emptyList()
        )
        return AccountDetail(levelDetail = accountLevelDetail)
    }
}
