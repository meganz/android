package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.dao.FolderPreferenceDao
import mega.privacy.android.data.database.entity.FolderPreferenceEntity
import mega.privacy.android.data.mapper.FolderPreferenceEntityMapper
import mega.privacy.android.data.mapper.FolderPreferenceMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.ViewType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderPreferenceRepositoryImplTest {
    private lateinit var underTest: FolderPreferenceRepositoryImpl

    private val folderPreferenceDao = mock<FolderPreferenceDao>()
    private val folderPreferenceMapper = mock<FolderPreferenceMapper>()
    private val folderPreferenceEntityMapper = mock<FolderPreferenceEntityMapper>()

    @BeforeAll
    fun setUp() {
        underTest = FolderPreferenceRepositoryImpl(
            folderPreferenceDao = folderPreferenceDao,
            folderPreferenceMapper = folderPreferenceMapper,
            folderPreferenceEntityMapper = folderPreferenceEntityMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(folderPreferenceDao, folderPreferenceMapper, folderPreferenceEntityMapper)
    }

    @Test
    fun `test that monitorFolderPreference maps the stored entity`() = runTest {
        val entity = FolderPreferenceEntity(
            folderKey = KEY,
            sortOrder = SORT_INT,
            viewType = ViewType.GRID.id
        )
        whenever(folderPreferenceDao.monitorByFolderKey(KEY)).thenReturn(flowOf(entity))
        whenever(folderPreferenceMapper(entity)).thenReturn(PREFERENCE)

        underTest.monitorFolderPreference(KEY).test {
            assertThat(awaitItem()).isEqualTo(PREFERENCE)
            awaitComplete()
        }
    }

    @Test
    fun `test that monitorFolderPreference emits null when there is no row`() = runTest {
        whenever(folderPreferenceDao.monitorByFolderKey(KEY)).thenReturn(flowOf(null))

        underTest.monitorFolderPreference(KEY).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    @Test
    fun `test that setFolderPreference writes the mapped entity without reading first`() = runTest {
        val entity = FolderPreferenceEntity(
            folderKey = KEY,
            sortOrder = SORT_INT,
            viewType = ViewType.GRID.id
        )
        whenever(folderPreferenceEntityMapper(PREFERENCE)).thenReturn(entity)

        underTest.setFolderPreference(PREFERENCE)

        verify(folderPreferenceDao).insertOrUpdate(entity)
        verify(folderPreferenceDao, never()).monitorByFolderKey(KEY)
    }

    @Test
    fun `test that clearFolderPreferences deletes all rows`() = runTest {
        underTest.clearFolderPreferences()

        verify(folderPreferenceDao).deleteAll()
    }

    companion object {
        private const val KEY = "1234567890"
        private const val SORT_INT = 4
        private val PREFERENCE = FolderPreference(
            folderKey = KEY,
            sortOrder = SortOrder.ORDER_SIZE_ASC,
            viewType = ViewType.GRID,
        )
    }
}
