package test.mega.privacy.android.app.lollipop.managerSections.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.ToggleAutoAcceptQRLinks
import mega.privacy.android.app.lollipop.managerSections.settings.SettingsViewModel
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsViewModelTest {
    private lateinit var underTest: SettingsViewModel

    private val fetchAutoAcceptQRLinks = mock<FetchAutoAcceptQRLinks>()
    private val toggleAutoAcceptQRLinks = mock<ToggleAutoAcceptQRLinks>()

    private val userAccount = UserAccount(
        email = "",
        isBusinessAccount = false,
        isMasterBusinessAccount = false,
        accountTypeIdentifier = 0
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = SettingsViewModel(
            getAccountDetails = mock { on { invoke(any()) }.thenReturn(userAccount) },
            canDeleteAccount = mock { on { invoke(userAccount) }.thenReturn(true) },
            refreshPasscodeLockPreference = mock(),
            isLoggingEnabled = mock(),
            isChatLoggingEnabled = mock(),
            isCameraSyncEnabled = mock(),
            rootNodeExists = mock { on { invoke() }.thenReturn(true) },
            isMultiFactorAuthAvailable = mock { on { invoke() }.thenReturn(true) },
            fetchAutoAcceptQRLinks = fetchAutoAcceptQRLinks,
            fetchMultiFactorAuthSetting = mock { on { invoke() }.thenReturn(emptyFlow()) },
            getStartScreen = mock(),
            shouldHideRecentActivity = mock(),
            toggleAutoAcceptQRLinks = toggleAutoAcceptQRLinks,
            isOnline = mock { on { invoke() }.thenReturn(emptyFlow()) },
        )
    }

    @Test
    fun `test initial value for auto accept is false`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().autoAcceptChecked).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the subsequent value auto accept is returned from the use case`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)
        underTest.uiState
            .map { it.autoAcceptChecked }
            .distinctUntilChanged(Boolean::equals)
            .test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }

    }

    @Test
    fun `test that toggle updates the auto accept value`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)
        whenever(toggleAutoAcceptQRLinks()).thenReturn(false)

        underTest.uiState
            .map { it.autoAcceptChecked }
            .distinctUntilChanged(Boolean::equals)
            .test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            underTest.toggleAutoAcceptPreference()
            assertThat(awaitItem()).isFalse()
        }

    }
}