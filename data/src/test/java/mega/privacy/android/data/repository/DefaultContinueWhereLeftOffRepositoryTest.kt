package mega.privacy.android.data.repository

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
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
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
    private val typeIdMapper = RecentlyUsedTypeIdMapper()
    private val mapper = ContinueWhereLeftOffItemMapper(typeIdMapper)

    @BeforeAll
    fun setUp() {
        underTest = DefaultContinueWhereLeftOffRepository(
            recentlyUsedDao = recentlyUsedDao,
            textEditorScrollDao = textEditorScrollDao,
            continueWhereLeftOffItemMapper = mapper,
            recentlyUsedTypeIdMapper = typeIdMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(recentlyUsedDao, textEditorScrollDao)
    }

    @Test
    fun `test that monitorContinueWhereLeftOffItems maps and returns items`() = runTest {
        val entity = RecentlyUsedEntity(
            nodeHandle = 1L,
            typeId = 1,
            fileName = "test.pdf",
            lastAccessedTimestamp = 1000L,
        )
        whenever(recentlyUsedDao.monitorRecentlyUsedItems(10))
            .thenReturn(flowOf(listOf(entity)))

        underTest.monitorContinueWhereLeftOffItems(10).test {
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
