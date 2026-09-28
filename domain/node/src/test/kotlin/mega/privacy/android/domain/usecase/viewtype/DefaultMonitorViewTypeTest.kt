package mega.privacy.android.domain.usecase.viewtype

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.repository.ViewTypeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorViewType]
 */
@ExperimentalCoroutinesApi
class DefaultMonitorViewTypeTest {
    private lateinit var underTest: MonitorViewType

    private val viewTypeRepository = mock<ViewTypeRepository>()

    @Before
    fun setUp() {
        underTest = DefaultMonitorViewType(
            viewTypeRepository = viewTypeRepository,
        )
    }

    @Test
    fun `test that a specific ViewType is returned`() = runTest {
        val viewTypes = ViewType.values()
        whenever(viewTypeRepository.monitorViewType()).thenReturn(
            viewTypes.asFlow()
        )

        underTest().test {
            viewTypes.forEach {
                assertThat(awaitItem()).isEqualTo(it)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a default value of LIST is returned if no value is set`() = runTest {
        whenever(viewTypeRepository.monitorViewType()).thenReturn(flowOf(null))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ViewType.LIST)
            cancelAndIgnoreRemainingEvents()
        }
    }
}