package test.mega.privacy.android.app.presentation.inbox

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.inbox.InboxViewModel
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaApiJava
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

/**
 * Test class for [InboxViewModel]
 */
@ExperimentalCoroutinesApi
class InboxViewModelTest {
    private lateinit var underTest: InboxViewModel

    private val monitorNodeUpdates = mock<MonitorNodeUpdates>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /**
     * This initializes the [InboxViewModel]
     */
    private fun setupUnderTest() {
        underTest = InboxViewModel(
            monitorNodeUpdates = monitorNodeUpdates,
            getCloudSortOrder = getCloudSortOrder,
            sortOrderIntMapper = sortOrderIntMapper,
        )
    }

    @Test
    fun `test that update nodes live data is not set when there are no updates triggered from use case`() =
        runTest {
            setupUnderTest()

            underTest.updateNodes.test().assertNoValue()
        }

    @Test
    fun `test that update nodes live data is set when there are updates triggered from use case`() =
        runTest {
            whenever(monitorNodeUpdates()).thenReturn(flowOf(listOf(mock())))

            setupUnderTest()

            runCatching {
                underTest.updateNodes.test().awaitValue(50, TimeUnit.MILLISECONDS)
            }.onSuccess { result ->
                result.assertValue { it.getContentIfNotHandled()?.size == 1 }
            }
        }

    @Test
    fun `test that get order returns cloud sort order`() = runTest {
        setupUnderTest()
        val order = SortOrder.ORDER_SIZE_DESC
        val expected = MegaApiJava.ORDER_SIZE_DESC
        whenever(getCloudSortOrder()).thenReturn(order)
        whenever(sortOrderIntMapper(order)).thenReturn(expected)
        assertThat(underTest.getOrder()).isEqualTo(expected)
    }
}