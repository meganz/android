package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.database.entity.FolderPreferenceEntity
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.ViewType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderPreferenceMapperTest {
    private lateinit var underTest: FolderPreferenceMapper

    private val sortOrderMapper = mock<SortOrderMapper>()

    @BeforeAll
    fun setUp() {
        underTest = FolderPreferenceMapper(sortOrderMapper)
    }

    @Test
    fun `test that the entity is mapped to a preference`() {
        whenever(sortOrderMapper(SORT_INT)).thenReturn(SortOrder.ORDER_SIZE_ASC)

        val result = underTest(
            FolderPreferenceEntity(
                folderKey = KEY,
                sortOrder = SORT_INT,
                viewType = ViewType.GRID.id,
            )
        )

        assertThat(result).isEqualTo(
            FolderPreference(
                folderKey = KEY,
                sortOrder = SortOrder.ORDER_SIZE_ASC,
                viewType = ViewType.GRID,
            )
        )
    }

    @Test
    fun `test that a null sort order falls back to the default ascending order`() {
        whenever(sortOrderMapper(UNKNOWN_SORT_INT)).thenReturn(null)

        val result = underTest(
            FolderPreferenceEntity(
                folderKey = KEY,
                sortOrder = UNKNOWN_SORT_INT,
                viewType = ViewType.LIST.id,
            )
        )

        assertThat(result.sortOrder).isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
    }

    @Test
    fun `test that an unknown view type falls back to list`() {
        whenever(sortOrderMapper(SORT_INT)).thenReturn(SortOrder.ORDER_SIZE_ASC)

        val result = underTest(
            FolderPreferenceEntity(
                folderKey = KEY,
                sortOrder = SORT_INT,
                viewType = UNKNOWN_VIEW_TYPE_ID,
            )
        )

        assertThat(result.viewType).isEqualTo(ViewType.LIST)
    }

    companion object {
        private const val KEY = "1234567890"
        private const val SORT_INT = 4
        private const val UNKNOWN_SORT_INT = -1
        private const val UNKNOWN_VIEW_TYPE_ID = 99
    }
}
