package mega.privacy.android.domain.usecase.folderpreference

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FolderPreferenceRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearFolderPreferencesUseCaseTest {
    private lateinit var underTest: ClearFolderPreferencesUseCase
    private val folderPreferenceRepository = mock<FolderPreferenceRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearFolderPreferencesUseCase(folderPreferenceRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(folderPreferenceRepository)
    }

    @Test
    fun `test that invoke clears the preferences on the repository`() = runTest {
        underTest()

        verify(folderPreferenceRepository).clearFolderPreferences()
    }
}
