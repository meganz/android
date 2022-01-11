package test.mega.privacy.android.app.lollipop.managerSections.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.ToggleAutoAcceptQRLinks
import mega.privacy.android.app.lollipop.managerSections.settings.SettingsViewModel
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsViewModelTest {
    private lateinit var underTest: SettingsViewModel

    private val fetchAutoAcceptQRLinks = mock<FetchAutoAcceptQRLinks>()
    private val toggleAutoAcceptQRLinks = mock<ToggleAutoAcceptQRLinks>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = SettingsViewModel(
            getAccountDetails = mock(),
            canDeleteAccount = mock(),
            refreshUserAccount = mock(),
            refreshPasscodeLockPreference = mock(),
            isLoggingEnabled = mock(),
            isChatLoggingEnabled = mock(),
            isCameraSyncEnabled = mock(),
            rootNodeExists = mock(),
            isMultiFactorAuthAvailable = mock(),
            fetchAutoAcceptQRLinks = fetchAutoAcceptQRLinks,
            performMultiFactorAuthCheck = mock(),
            getStartScreen = mock(),
            shouldHideRecentActivity = mock(),
            toggleAutoAcceptQRLinks = toggleAutoAcceptQRLinks,
        )
    }

    @Test
    fun `test initial value for auto accept is false`() = runTest {
        val actual = underTest.isAutoExceptEnabled.first()
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that the subsequent value auto accept is returned from the use case`() = runTest{
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)
        underTest.isAutoExceptEnabled.test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }

    }

    @Test
    fun `test that toggle updates the auto accept value`() = runTest{
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)
        whenever(toggleAutoAcceptQRLinks()).thenReturn(false)

        underTest.isAutoExceptEnabled.test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            underTest.toggleAutoAcceptPreference()
            assertThat(awaitItem()).isFalse()
        }

    }
}