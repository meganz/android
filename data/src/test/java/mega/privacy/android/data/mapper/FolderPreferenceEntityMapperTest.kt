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
class FolderPreferenceEntityMapperTest {
    private lateinit var underTest: FolderPreferenceEntityMapper

    private val sortOrderIntMapper = mock<SortOrderIntMapper>()

    @BeforeAll
    fun setUp() {
        underTest = FolderPreferenceEntityMapper(sortOrderIntMapper)
    }

    @Test
    fun `test that the preference is mapped to an entity`() {
        whenever(sortOrderIntMapper(SortOrder.ORDER_SIZE_ASC)).thenReturn(SORT_INT)

        val result = underTest(
            FolderPreference(
                folderKey = KEY,
                sortOrder = SortOrder.ORDER_SIZE_ASC,
                viewType = ViewType.GRID,
            )
        )

        assertThat(result).isEqualTo(
            FolderPreferenceEntity(
                folderKey = KEY,
                sortOrder = SORT_INT,
                viewType = ViewType.GRID.id,
            )
        )
    }

    companion object {
        private const val KEY = "1234567890"
        private const val SORT_INT = 4
    }
}
