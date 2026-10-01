package mega.privacy.android.domain.usecase.folderpreference

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorFolderPreferenceUseCaseTest {
    private lateinit var underTest: MonitorFolderPreferenceUseCase
    private val folderPreferenceRepository = mock<FolderPreferenceRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorFolderPreferenceUseCase(folderPreferenceRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(folderPreferenceRepository)
    }

    @Test
    fun `test that invoke emits the preference monitored by the repository`() = runTest {
        whenever(folderPreferenceRepository.monitorFolderPreference(KEY))
            .thenReturn(flowOf(PREFERENCE))

        underTest(KEY).test {
            assertThat(awaitItem()).isEqualTo(PREFERENCE)
            awaitComplete()
        }
    }

    companion object {
        private const val KEY = "1234567890"
        private val PREFERENCE = FolderPreference(
            folderKey = KEY,
            sortOrder = SortOrder.ORDER_SIZE_ASC,
            viewType = ViewType.GRID,
        )
    }
}
