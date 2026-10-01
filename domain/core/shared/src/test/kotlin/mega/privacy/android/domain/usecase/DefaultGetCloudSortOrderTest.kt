package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetCloudSortOrderTest {
    lateinit var underTest: GetCloudSortOrder
    private val sortOrderRepository = mock<SortOrderRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetCloudSortOrder(sortOrderRepository)
    }

    @Test
    fun `test that default value is returned when repository returns null`() {
        runTest {
            val expected = SortOrder.ORDER_DEFAULT_ASC
            whenever(sortOrderRepository.getCloudSortOrder()).thenReturn(
                null
            )
            assertThat(underTest()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that returned value matches when repository returns non null value`() {
        runTest {
            val expected = SortOrder.ORDER_FAV_ASC
            whenever(sortOrderRepository.getCloudSortOrder()).thenReturn(
                expected
            )
            assertThat(underTest()).isEqualTo(expected)
        }
    }
}