package mega.privacy.android.domain.usecase.node.sort

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MonitorLinksSortOrderUseCaseTest {

    private val sortOrderRepository = mock<SortOrderRepository>()
    private val underTest = MonitorLinksSortOrderUseCase(sortOrderRepository)

    @Test
    fun `test that invoke emits the links sort order from the repository`() = runTest {
        whenever(sortOrderRepository.monitorLinksSortOrder())
            .thenReturn(flowOf(SortOrder.ORDER_SIZE_ASC))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SortOrder.ORDER_SIZE_ASC)
            awaitComplete()
        }
    }
}
