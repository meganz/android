package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.media.MediaTimelineSort
import mega.privacy.android.domain.entity.media.MediaTimelineSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetTimelineSortUseCaseTest {

    private lateinit var underTest: SetTimelineSortUseCase

    private val settingsRepository: SettingsRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = SetTimelineSortUseCase(settingsRepository = settingsRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke persists the sort through the settings repository`() = runTest {
        val sort = MediaTimelineSort(MediaTimelineSortField.DateAdded, SortDirection.Ascending)

        underTest(sort)

        verify(settingsRepository).setTimelineSort(sort)
    }
}
