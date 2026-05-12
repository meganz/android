package mega.privacy.android.domain.usecase.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorContinueWhereLeftOffItemsUseCaseTest {

    private lateinit var underTest: MonitorContinueWhereLeftOffItemsUseCase

    private val repository = mock<ContinueWhereLeftOffRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorContinueWhereLeftOffItemsUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke returns items from repository`() = runTest {
        val items = listOf(
            ContinueWhereLeftOffItem(
                nodeHandle = 1L,
                type = RecentlyUsedType.PDF,
                title = "test.pdf",
                lastAccessedTimestamp = 1000L,
            )
        )
        whenever(repository.monitorContinueWhereLeftOffItems(10)).thenReturn(flowOf(items))

        underTest(10).test {
            assertThat(awaitItem()).isEqualTo(items)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).monitorContinueWhereLeftOffItems(10)
    }

    @Test
    fun `test that invoke returns empty list when no items`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(10)).thenReturn(flowOf(emptyList()))

        underTest(10).test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke forwards limit to repository`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(20)).thenReturn(flowOf(emptyList()))

        underTest(20).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).monitorContinueWhereLeftOffItems(20)
    }
}
