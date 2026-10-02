package mega.privacy.android.shared.account.inactivity

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountInactivity
import mega.privacy.android.domain.usecase.account.AcknowledgeLastPurgeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountInactivityUseCase
import mega.privacy.android.domain.usecase.account.SuppressPurgeTimestampUseCase
import mega.privacy.mobile.analytics.event.InactivityPurgeBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.InactivityPurgeBannerDisplayedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InactivityBannerViewModelTest {

    @JvmField
    @RegisterExtension
    val analyticsExtension = AnalyticsTestExtension()

    private val monitorAccountInactivityUseCase = mock<MonitorAccountInactivityUseCase>()
    private val acknowledgeLastPurgeUseCase = mock<AcknowledgeLastPurgeUseCase>()
    private val suppressPurgeTimestampUseCase = mock<SuppressPurgeTimestampUseCase>()
    private val inactivity = MutableStateFlow<AccountInactivity?>(null)

    @BeforeEach
    fun setUp() {
        reset(monitorAccountInactivityUseCase, acknowledgeLastPurgeUseCase, suppressPurgeTimestampUseCase)
        inactivity.value = null
        whenever(monitorAccountInactivityUseCase()).thenReturn(inactivity)
    }

    private fun TestScope.createViewModel(applicationScope: CoroutineScope = this) =
        InactivityBannerViewModel(
            monitorAccountInactivityUseCase = monitorAccountInactivityUseCase,
            acknowledgeLastPurgeUseCase = acknowledgeLastPurgeUseCase,
            suppressPurgeTimestampUseCase = suppressPurgeTimestampUseCase,
            applicationScope = applicationScope,
        )

    @Test
    fun `test that the banner is hidden while no purge event has been received`() = runTest {
        val underTest = createViewModel()
        runCurrent()

        assertThat(underTest.uiState.value.isVisible).isFalse()
        assertThat(analyticsExtension.events).doesNotContain(InactivityPurgeBannerDisplayedEvent)
    }

    @Test
    fun `test that a purge event shows the banner and tracks the displayed event once`() = runTest {
        val underTest = createViewModel()

        inactivity.value = AccountInactivity(inactivityMonths = 2, purgeTimestamp = 123L)
        runCurrent()
        inactivity.value = AccountInactivity(inactivityMonths = 3, purgeTimestamp = 123L)
        runCurrent()

        assertThat(underTest.uiState.value.isVisible).isTrue()
        assertThat(underTest.uiState.value.inactivityMonths).isEqualTo(3)
        assertThat(underTest.uiState.value.purgeTimestamp).isEqualTo(123L)
        assertThat(analyticsExtension.events.count { it == InactivityPurgeBannerDisplayedEvent }).isEqualTo(1)
    }

    @Test
    fun `test that dismiss suppresses and acknowledges the purge and tracks the close event`() = runTest {
        val underTest = createViewModel()

        underTest.dismiss(purgeTimestamp = 123L)
        advanceUntilIdle()

        verify(suppressPurgeTimestampUseCase).invoke(123L)
        verify(acknowledgeLastPurgeUseCase).invoke(123L)
        assertThat(analyticsExtension.events).contains(InactivityPurgeBannerCloseButtonPressedEvent)
    }

    @Test
    fun `test that the acknowledgement survives the ViewModel being cleared`() = runTest {
        val applicationScope = CoroutineScope(coroutineContext)
        val underTest = createViewModel(applicationScope = applicationScope)

        underTest.dismiss(purgeTimestamp = 123L)
        underTest.viewModelScope.cancel()
        advanceUntilIdle()

        verify(acknowledgeLastPurgeUseCase).invoke(123L)
    }

    @Test
    fun `test that the banner hides again when the purge event is cleared`() = runTest {
        val underTest = createViewModel()
        inactivity.value = AccountInactivity(inactivityMonths = 2, purgeTimestamp = 123L)
        runCurrent()

        inactivity.value = null
        runCurrent()

        assertThat(underTest.uiState.value.isVisible).isFalse()
    }
}
