package mega.privacy.android.data.repository

import androidx.sqlite.db.SupportSQLiteQuery
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.dao.RecentlyUsedDao
import mega.privacy.android.data.database.dao.TextEditorScrollDao
import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.data.database.entity.TextEditorScrollEntity
import mega.privacy.android.data.mapper.continuewhereleftoff.ContinueWhereLeftOffItemMapper
import mega.privacy.android.data.mapper.continuewhereleftoff.RecentlyUsedTypeIdMapper
import mega.privacy.android.data.preferences.ContinueWhereLeftOffSortPreferenceDataStore
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.entity.node.SortDirection
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultContinueWhereLeftOffRepositoryTest {
    private lateinit var underTest: DefaultContinueWhereLeftOffRepository

    private val recentlyUsedDao = mock<RecentlyUsedDao>()
    private val textEditorScrollDao = mock<TextEditorScrollDao>()
    private val sortPreferenceDataStore = mock<ContinueWhereLeftOffSortPreferenceDataStore>()
    private val typeIdMapper = RecentlyUsedTypeIdMapper()
    private val mapper = ContinueWhereLeftOffItemMapper(typeIdMapper)

    @BeforeAll
    fun setUp() {
        underTest = DefaultContinueWhereLeftOffRepository(
            recentlyUsedDao = recentlyUsedDao,
            textEditorScrollDao = textEditorScrollDao,
            continueWhereLeftOffItemMapper = mapper,
            recentlyUsedTypeIdMapper = typeIdMapper,
            sortPreferenceDataStore = sortPreferenceDataStore,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(recentlyUsedDao, textEditorScrollDao, sortPreferenceDataStore)
    }

    private fun stubSortPreference(
        sortField: ContinueWhereLeftOffSortField = ContinueWhereLeftOffSortField.Timestamp,
        sortDirection: SortDirection = SortDirection.Descending,
    ) {
        whenever(sortPreferenceDataStore.monitorSortPreference())
            .thenReturn(flowOf(sortField to sortDirection))
    }

    @Test
    fun `test that monitorContinueWhereLeftOffItems maps and returns items`() = runTest {
        val entity = RecentlyUsedEntity(
            nodeHandle = 1L,
            typeId = 1,
            fileName = "test.pdf",
            lastAccessedTimestamp = 1000L,
        )
        stubSortPreference()
        whenever(recentlyUsedDao.monitorItems(any()))
            .thenReturn(flowOf(listOf(entity)))

        underTest.monitorContinueWhereLeftOffItems(limit = 10).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().nodeHandle).isEqualTo(1L)
            assertThat(items.first().type).isEqualTo(RecentlyUsedType.PDF)
            assertThat(items.first().title).isEqualTo("test.pdf")
            assertThat(items.first().lastAccessedTimestamp).isEqualTo(1000L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorContinueWhereLeftOffItems builds name asc query when sort by name ascending`() =
        runTest {
            stubSortPreference(
                sortField = ContinueWhereLeftOffSortField.Name,
                sortDirection = SortDirection.Ascending,
            )
            whenever(recentlyUsedDao.monitorItems(any()))
                .thenReturn(flowOf(emptyList()))

            underTest.monitorContinueWhereLeftOffItems(limit = 10).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(capturedQuery().sql).isEqualTo(
                "SELECT * FROM recently_used ORDER BY file_name COLLATE NOCASE ASC LIMIT ?"
            )
        }

    @Test
    fun `test that monitorContinueWhereLeftOffItems builds name desc query when sort by name descending`() =
        runTest {
            stubSortPreference(
                sortField = ContinueWhereLeftOffSortField.Name,
                sortDirection = SortDirection.Descending,
            )
            whenever(recentlyUsedDao.monitorItems(any()))
                .thenReturn(flowOf(emptyList()))

            underTest.monitorContinueWhereLeftOffItems(limit = 10).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(capturedQuery().sql).isEqualTo(
                "SELECT * FROM recently_used ORDER BY file_name COLLATE NOCASE DESC LIMIT ?"
            )
        }

    @Test
    fun `test that monitorContinueWhereLeftOffItems builds timestamp asc query when sort by timestamp ascending`() =
        runTest {
            stubSortPreference(
                sortField = ContinueWhereLeftOffSortField.Timestamp,
                sortDirection = SortDirection.Ascending,
            )
            whenever(recentlyUsedDao.monitorItems(any()))
                .thenReturn(flowOf(emptyList()))

            underTest.monitorContinueWhereLeftOffItems(limit = 10).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(capturedQuery().sql).isEqualTo(
                "SELECT * FROM recently_used ORDER BY last_accessed_timestamp ASC LIMIT ?"
            )
        }

    @Test
    fun `test that monitorContinueWhereLeftOffItems builds timestamp desc query when sort by timestamp descending`() =
        runTest {
            stubSortPreference(
                sortField = ContinueWhereLeftOffSortField.Timestamp,
                sortDirection = SortDirection.Descending,
            )
            whenever(recentlyUsedDao.monitorItems(any()))
                .thenReturn(flowOf(emptyList()))

            underTest.monitorContinueWhereLeftOffItems(limit = 10).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(capturedQuery().sql).isEqualTo(
                "SELECT * FROM recently_used ORDER BY last_accessed_timestamp DESC LIMIT ?"
            )
        }

    @Test
    fun `test that monitorSortPreference delegates to datastore`() = runTest {
        stubSortPreference(
            sortField = ContinueWhereLeftOffSortField.Name,
            sortDirection = SortDirection.Descending,
        )

        underTest.monitorSortPreference().test {
            val (field, direction) = awaitItem()
            assertThat(field).isEqualTo(ContinueWhereLeftOffSortField.Name)
            assertThat(direction).isEqualTo(SortDirection.Descending)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setSortPreference delegates to datastore`() = runTest {
        underTest.setSortPreference(
            sortField = ContinueWhereLeftOffSortField.Name,
            sortDirection = SortDirection.Ascending,
        )

        verify(sortPreferenceDataStore).setSortPreference(
            ContinueWhereLeftOffSortField.Name,
            SortDirection.Ascending,
        )
    }

    private fun capturedQuery(): SupportSQLiteQuery {
        val captor = argumentCaptor<SupportSQLiteQuery>()
        verify(recentlyUsedDao).monitorItems(captor.capture())
        return captor.firstValue
    }

    @Test
    fun `test that saveRecentlyUsedItem calls insertAndPrune with correct type`() = runTest {
        underTest.saveRecentlyUsedItem(1L, RecentlyUsedType.Video, "video.mp4")

        verify(recentlyUsedDao).insertAndPrune(any(), any())
    }

    @Test
    fun `test that savePosition updates timestamp for existing item`() = runTest {
        val existing = RecentlyUsedEntity(
            nodeHandle = 1L,
            typeId = 2,
            fileName = "video.mp4",
            lastAccessedTimestamp = 1000L,
        )
        whenever(recentlyUsedDao.getByNodeHandle(1L)).thenReturn(existing)

        underTest.savePosition(1L)

        verify(recentlyUsedDao).insertOrUpdate(any())
    }

    @Test
    fun `test that savePosition does nothing when item does not exist`() = runTest {
        whenever(recentlyUsedDao.getByNodeHandle(1L)).thenReturn(null)

        underTest.savePosition(1L)

        verify(recentlyUsedDao).getByNodeHandle(1L)
    }

    @Test
    fun `test that removeRecentlyUsedItem calls deleteByNodeHandle`() = runTest {
        underTest.removeRecentlyUsedItem(1L)

        verify(recentlyUsedDao).deleteByNodeHandle(1L)
    }

    @Test
    fun `test that clearAllRecentlyUsedItems calls deleteAll`() = runTest {
        underTest.clearAllRecentlyUsedItems()

        verify(recentlyUsedDao).deleteAll()
    }

    @Test
    fun `test that saveTextEditorScroll calls dao insertOrUpdate`() = runTest {
        val scroll = TextEditorScroll(
            nodeHandle = 1L,
            cursorPosition = 100,
            scrollFraction = 0.5f,
        )

        underTest.saveTextEditorScroll(scroll)

        verify(textEditorScrollDao).insertOrUpdate(any())
    }

    @Test
    fun `test that getTextEditorScroll returns mapped entity when exists`() = runTest {
        whenever(textEditorScrollDao.getByNodeHandle(1L)).thenReturn(
            TextEditorScrollEntity(
                nodeHandle = 1L,
                cursorPosition = 100,
                scrollSpot = 0.5f,
            )
        )

        val result = underTest.getTextEditorScroll(1L)

        assertThat(result).isNotNull()
        assertThat(result?.nodeHandle).isEqualTo(1L)
        assertThat(result?.cursorPosition).isEqualTo(100)
        assertThat(result?.scrollFraction).isEqualTo(0.5f)
    }

    @Test
    fun `test that getTextEditorScroll returns null when not found`() = runTest {
        whenever(textEditorScrollDao.getByNodeHandle(1L)).thenReturn(null)

        assertThat(underTest.getTextEditorScroll(1L)).isNull()
    }

    @Test
    fun `test that deleteTextEditorScroll calls dao deleteByNodeHandle`() = runTest {
        underTest.deleteTextEditorScroll(1L)

        verify(textEditorScrollDao).deleteByNodeHandle(1L)
    }
}
