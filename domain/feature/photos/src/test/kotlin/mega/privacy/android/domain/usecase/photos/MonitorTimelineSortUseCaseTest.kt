package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTimelineSortUseCaseTest {

    private lateinit var underTest: MonitorTimelineSortUseCase

    private val settingsRepository: SettingsRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = MonitorTimelineSortUseCase(settingsRepository = settingsRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke emits the persisted sort`() = runTest {
        val sort = MediaTimelineSort(MediaTimelineSortField.DateTaken, SortDirection.Ascending)
        whenever(settingsRepository.monitorTimelineSort()).thenReturn(flowOf(sort))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(sort)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits the default sort when no sort is persisted`() = runTest {
        whenever(settingsRepository.monitorTimelineSort()).thenReturn(flowOf(null))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(MediaTimelineSort.Default)
            awaitComplete()
        }
    }

    @Test
    fun `test that the default sort is date added newest first`() {
        assertThat(MediaTimelineSort.Default).isEqualTo(
            MediaTimelineSort(MediaTimelineSortField.DateAdded, SortDirection.Descending)
        )
    }
}
