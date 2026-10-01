package mega.privacy.android.domain.usecase.media

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorMediaSystemAlbumsUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorMediaSystemAlbumsUseCaseTest {
    private lateinit var underTest: MonitorMediaSystemAlbumsUseCase
    private val testDispatcher = UnconfinedTestDispatcher()

    private val listMediaNodesByOffsetUseCase: ListMediaNodesByOffsetUseCase = mock()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
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
            listMediaNodesByOffsetUseCase,
            monitorNodeUpdatesUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase,
        )
    }

    private fun initUseCase(systemAlbums: Set<SystemAlbum>) {
        underTest = MonitorMediaSystemAlbumsUseCase(
            systemAlbums = systemAlbums,
            listMediaNodesByOffsetUseCase = listMediaNodesByOffsetUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            defaultDispatcher = testDispatcher,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        )
    }

    private fun stubTriggers(showHiddenItems: Boolean, isPaid: Boolean) {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(emptyFlow())
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

    private suspend fun verifyCoverQueries(
        album: SystemAlbum,
        sensitivity: MediaTimelineFilter.Sensitivity,
        count: Int,
    ) {
        verify(listMediaNodesByOffsetUseCase, times(count))(
            filter = album.mediaTimelineFilter.copy(sensitivity = sensitivity),
            section = null,
            order = SortOrder.ORDER_MODIFICATION_DESC,
            maxElements = 1,
            offset = 0,
        )
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
    fun `test that paid account with hidden items enabled applies ShowAll to the filter`() =
        runTest {
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

    @Test
    fun `test that empty system album set emits an empty list`() = runTest {
        stubTriggers(showHiddenItems = true, isPaid = true)

        initUseCase(emptySet())

        underTest().test {
            assertThat(awaitItem()).isEmpty()

            awaitComplete()
        }
    }

    @Test
    fun `test that favouriting a media node only reloads the favourite scoped album`() = runTest {
        val nodeUpdates = stubHotTriggers()
        stubChangingCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            nodeUpdates.emit(mediaNodeUpdate(NodeChanges.Favourite))
            awaitItem()

            verifyCoverQueries(gifAlbum, MediaTimelineFilter.Sensitivity.ShowAll, 1)
            verifyCoverQueries(rawAlbum, MediaTimelineFilter.Sensitivity.ShowAll, 1)
            verifyCoverQueries(favouriteAlbum, MediaTimelineFilter.Sensitivity.ShowAll, 2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "when the media node change is {0}")
    @EnumSource(
        value = NodeChanges::class,
        names = ["New", "Remove", "Parent", "Sensitive", "Timestamp", "Name"],
    )
    fun `test that a media node change reloads every album`(change: NodeChanges) = runTest {
        val nodeUpdates = stubHotTriggers()
        stubChangingCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            nodeUpdates.emit(mediaNodeUpdate(change))
            awaitItem()

            verifyAllAlbumsQueried(times = 2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "when the media node change is {0}")
    @EnumSource(
        value = NodeChanges::class,
        names = ["Attributes", "Description", "Tags", "Owner", "Outshare"],
    )
    fun `test that a media node change that no filter can see reloads nothing`(
        change: NodeChanges,
    ) = runTest {
        val nodeUpdates = stubHotTriggers()
        stubAllCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            nodeUpdates.emit(mediaNodeUpdate(change))
            expectNoEvents()

            verifyAllAlbumsQueried(times = 1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a non media file change reloads nothing`() = runTest {
        val nodeUpdates = stubHotTriggers()
        stubAllCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            nodeUpdates.emit(
                nodeUpdate(createFileNode(PdfFileTypeInfo), NodeChanges.New, NodeChanges.Favourite)
            )
            expectNoEvents()

            verifyAllAlbumsQueried(times = 1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that renaming a non media file reloads every album`() = runTest {
        val nodeUpdates = stubHotTriggers()
        stubChangingCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            nodeUpdates.emit(nodeUpdate(createFileNode(PdfFileTypeInfo), NodeChanges.Name))
            awaitItem()

            verifyAllAlbumsQueried(times = 2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "when the folder change is {0}")
    @EnumSource(
        value = NodeChanges::class,
        names = ["New", "Remove", "Parent", "Sensitive"],
    )
    fun `test that a folder change altering a media subtree reloads every album`(
        change: NodeChanges,
    ) = runTest {
        val nodeUpdates = stubHotTriggers()
        stubChangingCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            nodeUpdates.emit(nodeUpdate(mock<FolderNode>(), change))
            awaitItem()

            verifyAllAlbumsQueried(times = 2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "when the folder change is {0}")
    @EnumSource(value = NodeChanges::class, names = ["Name", "Timestamp", "Favourite"])
    fun `test that a folder change that cannot alter a media subtree reloads nothing`(
        change: NodeChanges,
    ) = runTest {
        val nodeUpdates = stubHotTriggers()
        stubAllCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            nodeUpdates.emit(nodeUpdate(mock<FolderNode>(), change))
            expectNoEvents()

            verifyAllAlbumsQueried(times = 1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that account detail updates that do not change the paid status reload nothing`() =
        runTest {
            val accountDetail = MutableStateFlow(createMockAccountDetail(isPaid = true))
            stubHotTriggers(accountDetail = accountDetail)
            stubAllCovers()

            initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

            underTest().test {
                awaitItem()

                accountDetail.value = createMockAccountDetail(isPaid = true).copy(
                    storageDetail = AccountStorageDetail(
                        usedCloudDrive = 1L,
                        usedRubbish = 0L,
                        usedIncoming = 0L,
                        totalStorage = 100L,
                        usedStorage = 1L,
                    )
                )
                expectNoEvents()

                verifyAllAlbumsQueried(times = 1)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that toggling show hidden items on a paid account reloads albums with the new sensitivity`() =
        runTest {
            val showHiddenItems = MutableStateFlow(true)
            stubHotTriggers(showHiddenItems = showHiddenItems)
            stubAllCovers()
            stubAllCovers(MediaTimelineFilter.Sensitivity.HideSensitive)

            initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

            underTest().test {
                awaitItem()

                showHiddenItems.value = false
                awaitItem()

                verifyAllAlbumsQueried(times = 1, MediaTimelineFilter.Sensitivity.ShowAll)
                verifyAllAlbumsQueried(times = 1, MediaTimelineFilter.Sensitivity.HideSensitive)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that toggling show hidden items on a free account reloads nothing`() = runTest {
        val showHiddenItems = MutableStateFlow(true)
        stubHotTriggers(
            showHiddenItems = showHiddenItems,
            accountDetail = MutableStateFlow(createMockAccountDetail(isPaid = false)),
        )
        stubAllCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            showHiddenItems.value = false
            expectNoEvents()

            verifyAllAlbumsQueried(times = 1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that each collector loads its own covers`() = runTest {
        stubHotTriggers()
        stubAllCovers()

        initUseCase(setOf(gifAlbum, rawAlbum, favouriteAlbum))

        underTest().test {
            awaitItem()

            underTest().test {
                awaitItem()

                verifyAllAlbumsQueried(times = 2)

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that albums are not re-emitted when the reloaded covers are unchanged`() = runTest {
        val nodeUpdates = stubHotTriggers()
        stubAllCovers()

        initUseCase(setOf(favouriteAlbum))

        underTest().test {
            awaitItem()

            nodeUpdates.emit(mediaNodeUpdate(NodeChanges.Favourite))
            expectNoEvents()

            verifyCoverQueries(favouriteAlbum, MediaTimelineFilter.Sensitivity.ShowAll, 2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubHotTriggers(
        showHiddenItems: MutableStateFlow<Boolean> = MutableStateFlow(true),
        accountDetail: MutableStateFlow<AccountDetail> =
            MutableStateFlow(createMockAccountDetail(isPaid = true)),
    ): MutableSharedFlow<NodeUpdate> {
        val nodeUpdates = MutableSharedFlow<NodeUpdate>()
        whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdates)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(showHiddenItems)
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetail)
        return nodeUpdates
    }

    private suspend fun stubAllCovers(
        sensitivity: MediaTimelineFilter.Sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
    ) {
        stubCover(gifAlbum, sensitivity, mock())
        stubCover(rawAlbum, sensitivity, mock())
        stubCover(favouriteAlbum, sensitivity, mock())
    }

    /**
     * Each album returns a different cover on its second query, so a reload is observable as a
     * downstream emission rather than being swallowed by the output distinctUntilChanged.
     */
    private suspend fun stubChangingCovers() {
        listOf(gifAlbum, rawAlbum, favouriteAlbum).forEach { album ->
            whenever(
                listMediaNodesByOffsetUseCase(
                    filter = album.mediaTimelineFilter.copy(
                        sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
                    ),
                    section = null,
                    order = SortOrder.ORDER_MODIFICATION_DESC,
                    maxElements = 1,
                    offset = 0,
                )
            ).thenReturn(listOf(mock()), listOf(mock()))
        }
    }

    private suspend fun verifyAllAlbumsQueried(
        times: Int,
        sensitivity: MediaTimelineFilter.Sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
    ) {
        verifyCoverQueries(gifAlbum, sensitivity, times)
        verifyCoverQueries(rawAlbum, sensitivity, times)
        verifyCoverQueries(favouriteAlbum, sensitivity, times)
    }

    private fun mediaNodeUpdate(vararg changes: NodeChanges): NodeUpdate =
        nodeUpdate(createFileNode(StaticImageFileTypeInfo("image/jpeg", "jpg")), *changes)

    private fun nodeUpdate(node: Node, vararg changes: NodeChanges): NodeUpdate =
        NodeUpdate(mapOf(node to changes.toList()))

    private fun createFileNode(
        type: FileTypeInfo,
    ): FileNode = mock<FileNode> {
        on { this.type } doReturn type
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
