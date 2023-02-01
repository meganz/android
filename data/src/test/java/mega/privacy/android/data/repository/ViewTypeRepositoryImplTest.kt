package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.mapper.ViewTypeMapper
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.repository.ViewTypeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [ViewTypeRepositoryImpl]
 */
@ExperimentalCoroutinesApi
internal class ViewTypeRepositoryImplTest {

    private lateinit var underTest: ViewTypeRepository

    private val viewTypeMapper = mock<ViewTypeMapper> {
        on { it(0) }.thenReturn(ViewType.LIST)
        on { it(1) }.thenReturn(ViewType.GRID)
        on { it(2) }.thenReturn(null)
    }
    private val uiPreferencesGateway = mock<UIPreferencesGateway>()

    @Before
    fun setUp() {
        underTest = ViewTypeRepositoryImpl(
            viewTypeMapper = viewTypeMapper,
            uiPreferencesGateway = uiPreferencesGateway,
        )
    }

    private suspend fun testMonitorViewType(expectedResult: ViewType?) {
        underTest.monitorViewType().test {
            val result = awaitItem()
            assertThat(result).isEqualTo(expectedResult)
            awaitComplete()
        }
    }

    @Test
    fun `test that monitorViewType emits LIST`() = runTest {
        whenever(uiPreferencesGateway.monitorViewType()).thenReturn(flowOf(0))
        testMonitorViewType(ViewType.LIST)
    }

    @Test
    fun `test that monitorViewType emits GRID`() = runTest {
        whenever(uiPreferencesGateway.monitorViewType()).thenReturn(flowOf(1))
        testMonitorViewType(ViewType.GRID)
    }

    @Test
    fun `test that monitorViewType emits null if an invalid id is set`() = runTest {
        whenever(uiPreferencesGateway.monitorViewType()).thenReturn(flowOf(2))
        testMonitorViewType(null)
    }

    @Test
    fun `test that setViewType would invoke setViewType from uiPreferencesGateway`() =
        runTest {
            underTest.setViewType(ViewType.LIST)
            verify(uiPreferencesGateway).setViewType(ViewType.LIST.id)
        }
}