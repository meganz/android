package mega.privacy.android.app.presentation.actionmode

import com.jraska.livedata.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import mega.privacy.android.app.InstantExecutorExtension
import java.util.concurrent.TimeUnit

@ExtendWith(InstantExecutorExtension::class)
@ExperimentalCoroutinesApi
class ActionModeViewModelTest {

    private lateinit var underTest: ActionModeViewModel

    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()

    @Test
    fun `test that an action bar message is emitted if the transfers are paused`() =
        runTest {
            underTest = ActionModeViewModel(areTransfersPausedUseCase)
            whenever(areTransfersPausedUseCase()).thenReturn(true)
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
            underTest = ActionModeViewModel(areTransfersPausedUseCase)
            whenever(areTransfersPausedUseCase()).thenReturn(false)
            underTest.executeTransfer { return@executeTransfer }
            scheduler.advanceUntilIdle()

            underTest.onActionBarMessage().test().assertNoValue()
        }

    companion object {
        private val scheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(scheduler))
    }
}
