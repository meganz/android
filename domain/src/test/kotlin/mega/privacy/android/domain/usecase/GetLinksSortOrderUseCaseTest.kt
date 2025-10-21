package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLinksSortOrderUseCaseTest {
    lateinit var underTest: GetLinksSortOrderUseCase
    private val sortOrderRepository = mock<SortOrderRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetLinksSortOrderUseCase(sortOrderRepository)
    }

    @BeforeEach
    fun resetMocks() = reset(sortOrderRepository)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that default value is returned when repository returns null`(isSingleActivityEnabled: Boolean) {
        runTest {
            val expected = SortOrder.ORDER_DEFAULT_ASC
            whenever(sortOrderRepository.getLinksSortOrder(isSingleActivityEnabled)).thenReturn(
                null
            )
            assertThat(underTest(isSingleActivityEnabled)).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that returned value matches when repository returns non null value`(
        isSingleActivityEnabled: Boolean,
    ) {
        runTest {
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(sortOrderRepository.getLinksSortOrder(isSingleActivityEnabled)).thenReturn(
                expected
            )
            assertThat(underTest(isSingleActivityEnabled)).isEqualTo(expected)
        }
    }
}