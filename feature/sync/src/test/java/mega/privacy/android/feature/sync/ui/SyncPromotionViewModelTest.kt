package mega.privacy.android.feature.sync.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.domain.usecase.SetSyncPromotionShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.ShouldShowSyncPromotionUseCase
import mega.privacy.android.feature.sync.ui.views.SyncPromotionViewModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncPromotionViewModelTest {

    private lateinit var underTest: SyncPromotionViewModel

    private val shouldShowSyncPromotionUseCase: ShouldShowSyncPromotionUseCase = mock()
    private val setSyncPromotionShownUseCase: SetSyncPromotionShownUseCase = mock()

    private fun initViewModel() {
        underTest = SyncPromotionViewModel(
            shouldShowSyncPromotionUseCase = shouldShowSyncPromotionUseCase,
            setSyncPromotionShownUseCase = setSyncPromotionShownUseCase,
        )
    }

    @BeforeEach
    fun reset() {
        reset(
            shouldShowSyncPromotionUseCase,
            setSyncPromotionShownUseCase,
        )

        initViewModel()
    }

    @Test
    fun `test that the flag to indicate if Sync Promotion should be shown is set`() = runTest {
        whenever(shouldShowSyncPromotionUseCase()).thenReturn(true)
        initViewModel()
        testScheduler.advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().shouldShowSyncPromotion).isTrue()
        }
    }

    @Test
    fun `test that the flag to indicate if Sync Promotion should be shown is reset`() = runTest {
        underTest.onConsumeShouldShowSyncPromotion()
        testScheduler.advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().shouldShowSyncPromotion).isFalse()
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
