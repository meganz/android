package mega.privacy.android.app.presentation.overdisk

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.GetNumberOfNodesUseCase
import mega.privacy.android.domain.usecase.account.GetOverDiskQuotaDeadlineUseCase
import mega.privacy.android.domain.usecase.account.GetOverDiskQuotaWarningTimestampsUseCase
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OverDiskQuotaPaywallViewModelTest {

    private lateinit var underTest: OverDiskQuotaPaywallViewModel

    private val isDatabaseEntryStale: IsDatabaseEntryStale = mock()
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase = mock()
    private val getPricing: GetPricing = mock()
    private val getUserDataUseCase: GetUserDataUseCase = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()
    private val getNumberOfNodesUseCase: GetNumberOfNodesUseCase = mock()
    private val getOverDiskQuotaDeadlineUseCase: GetOverDiskQuotaDeadlineUseCase = mock()
    private val getOverDiskQuotaWarningTimestampsUseCase: GetOverDiskQuotaWarningTimestampsUseCase =
        mock()
    private val monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase = mock()
    private val myAccountUpdateFlow = MutableStateFlow(
        MyAccountUpdate(MyAccountUpdate.Action.STORAGE_STATE_CHANGED)
    )
    private val monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase = mock()

    @BeforeEach
    fun setup() = runTest {
        whenever(monitorUpdateUserDataUseCase()).thenReturn(emptyFlow())
        whenever(monitorMyAccountUpdateUseCase()).thenReturn(myAccountUpdateFlow)
        whenever(monitorAccountDetailUseCase()).thenReturn(emptyFlow())
        whenever(monitorThemeModeUseCase()).thenReturn(flowOf(ThemeMode.System))
        whenever(getPricing(false)).thenReturn(Pricing(emptyList()))
        whenever(isDatabaseEntryStale()).thenReturn(false)
        whenever(getCurrentUserEmail()).thenReturn("")
        whenever(getNumberOfNodesUseCase()).thenReturn(0L)
        whenever(getOverDiskQuotaWarningTimestampsUseCase()).thenReturn(emptyList())
        whenever(getOverDiskQuotaDeadlineUseCase()).thenReturn(-1L)
        initializeViewModel()
    }

    @Test
    fun `test that pricing products populate the ui state`() = runTest {
        val products = listOf(
            Product(
                handle = 1L,
                level = 1,
                months = 1,
                storage = 100,
                transfer = 100,
                amount = 100,
                currency = null,
                isBusiness = false,
            )
        )
        whenever(getPricing(false)).thenReturn(Pricing(products))

        initializeViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().products).isEqualTo(products)
        }
    }

    @Test
    fun `test that used storage from monitor account detail populates the ui state`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(
            flowOf(
                AccountDetail(
                    storageDetail = AccountStorageDetail(
                        usedCloudDrive = 0,
                        usedRubbish = 0,
                        usedIncoming = 0,
                        totalStorage = 0,
                        usedStorage = 1024L,
                    )
                )
            )
        )

        initializeViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().usedStorage).isEqualTo(1024L)
        }
    }

    @Test
    fun `test that account data populates the ui state on init`() = runTest {
        whenever(getCurrentUserEmail()).thenReturn("user@mega.co.nz")
        whenever(getNumberOfNodesUseCase()).thenReturn(42L)
        whenever(getOverDiskQuotaWarningTimestampsUseCase()).thenReturn(listOf(1L, 2L))
        whenever(getOverDiskQuotaDeadlineUseCase()).thenReturn(99L)

        initializeViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.email).isEqualTo("user@mega.co.nz")
            assertThat(state.fileCount).isEqualTo(42L)
            assertThat(state.warningTimestamps).isEqualTo(listOf(1L, 2L))
            assertThat(state.deadlineTimestamp).isEqualTo(99L)
        }
    }

    @Test
    fun `test that isLoading is false after account data is loaded`() = runTest {
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isLoading).isFalse()
        }
    }

    @Test
    fun `test that get specific account detail use case is executed when database entry is stale`() =
        runTest {
            whenever(isDatabaseEntryStale()).thenReturn(true)

            initializeViewModel()
            advanceUntilIdle()

            verify(getSpecificAccountDetailUseCase).invoke(
                storage = true,
                transfer = false,
                pro = false,
            )
        }

    @Test
    fun `test that get specific account detail use case is not executed when database entry is not stale`() =
        runTest {
            whenever(isDatabaseEntryStale()).thenReturn(false)

            initializeViewModel()
            advanceUntilIdle()

            verify(getSpecificAccountDetailUseCase, never()).invoke(
                storage = true,
                transfer = false,
                pro = false,
            )
        }

    @Test
    fun `test that get user data use case is executed on init`() = runTest {
        // The view model is already constructed in setup(); verify the init-time call.
        advanceUntilIdle()

        verify(getUserDataUseCase).invoke()
    }

    @Test
    fun `test that theme mode reflects the value from the monitor theme mode use case`() = runTest {
        whenever(monitorThemeModeUseCase()).thenReturn(flowOf(ThemeMode.Dark))

        initializeViewModel()
        advanceUntilIdle()

        underTest.themeMode.test {
            assertThat(expectMostRecentItem()).isEqualTo(ThemeMode.Dark)
        }
    }

    @AfterEach
    fun resetMocks() {
        reset(
            isDatabaseEntryStale,
            getSpecificAccountDetailUseCase,
            getPricing,
            getUserDataUseCase,
            getCurrentUserEmail,
            getNumberOfNodesUseCase,
            getOverDiskQuotaDeadlineUseCase,
            getOverDiskQuotaWarningTimestampsUseCase,
            monitorUpdateUserDataUseCase,
            monitorMyAccountUpdateUseCase,
            monitorAccountDetailUseCase,
            monitorThemeModeUseCase,
        )
    }

    private fun initializeViewModel() {
        underTest = OverDiskQuotaPaywallViewModel(
            isDatabaseEntryStale = isDatabaseEntryStale,
            getSpecificAccountDetailUseCase = getSpecificAccountDetailUseCase,
            getPricing = getPricing,
            getUserDataUseCase = getUserDataUseCase,
            getCurrentUserEmail = getCurrentUserEmail,
            getNumberOfNodesUseCase = getNumberOfNodesUseCase,
            getOverDiskQuotaDeadlineUseCase = getOverDiskQuotaDeadlineUseCase,
            getOverDiskQuotaWarningTimestampsUseCase = getOverDiskQuotaWarningTimestampsUseCase,
            monitorUpdateUserDataUseCase = monitorUpdateUserDataUseCase,
            monitorMyAccountUpdateUseCase = monitorMyAccountUpdateUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorThemeModeUseCase = monitorThemeModeUseCase,
        )
    }
}
