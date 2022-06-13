package test.mega.privacy.android.app.presentation.actionmode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.AreTransfersPaused
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class ActionModeViewModelTest {

    private lateinit var underTest: ActionModeViewModel

    private val areTransfersPaused = mock<AreTransfersPaused>()

    private val scheduler = TestCoroutineScheduler()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that an action bar message is emitted if the transfers are paused`() =
        runTest {
            underTest = ActionModeViewModel(areTransfersPaused)
            whenever(areTransfersPaused()).thenReturn(true)
            underTest.executeTransfer { return@executeTransfer }
            scheduler.advanceUntilIdle()

            runCatching {
                underTest.onActionBarMessage().test().awaitValue(50, TimeUnit.MILLISECONDS)
            }.onSuccess {
                it.assertHasValue()
            }
        }

    @Test
    fun `test that no action bar message is shown if the transfers are not paused`() =
        runTest {
            underTest = ActionModeViewModel(areTransfersPaused)
            whenever(areTransfersPaused()).thenReturn(false)
            underTest.executeTransfer { return@executeTransfer }
            scheduler.advanceUntilIdle()

            underTest.onActionBarMessage().test().assertNoValue()
        }
}
