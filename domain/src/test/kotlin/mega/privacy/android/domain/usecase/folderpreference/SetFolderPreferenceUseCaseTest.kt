package mega.privacy.android.domain.usecase.folderpreference

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.repository.FolderPreferenceRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetFolderPreferenceUseCaseTest {
    private lateinit var underTest: SetFolderPreferenceUseCase
    private val folderPreferenceRepository = mock<FolderPreferenceRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetFolderPreferenceUseCase(folderPreferenceRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(folderPreferenceRepository)
    }

    @Test
    fun `test that invoke sets the preference on the repository`() = runTest {
        val preference = FolderPreference(
            folderKey = "1234567890",
            sortOrder = SortOrder.ORDER_SIZE_ASC,
            viewType = ViewType.GRID,
        )

        underTest(preference)

        verify(folderPreferenceRepository).setFolderPreference(preference)
    }
}
