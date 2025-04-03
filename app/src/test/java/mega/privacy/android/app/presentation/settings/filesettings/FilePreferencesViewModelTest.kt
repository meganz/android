package mega.privacy.android.app.presentation.settings.filesettings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetFolderVersionInfo
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.cache.ClearCacheUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheSizeUseCase
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.filenode.RemoveAllVersionsUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.ClearOfflineUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFolderSizeUseCase
import mega.privacy.android.domain.usecase.setting.EnableFileVersionsOption
import mega.privacy.android.domain.usecase.setting.GetRubbishBinAutopurgePeriodUseCase
import mega.privacy.android.domain.usecase.setting.IsRubbishBinAutopurgeEnabledUseCase
import mega.privacy.android.domain.usecase.setting.IsRubbishBinAutopurgePeriodValidUseCase
import mega.privacy.android.domain.usecase.setting.SetRubbishBinAutopurgePeriodUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class FilePreferencesViewModelTest {
    private lateinit var underTest: FilePreferencesViewModel

    private val getFolderVersionInfo: GetFolderVersionInfo = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val getFileVersionsOption: GetFileVersionsOption = mock {
        onBlocking { invoke(any()) }.thenReturn(false)
    }
    private val fakeMonitorUserUpdates = MutableSharedFlow<UserChanges>()
    private val monitorUserUpdates: MonitorUserUpdates = mock {
        on { invoke() }.thenReturn(fakeMonitorUserUpdates)
    }
    private val enableFileVersionsOption: EnableFileVersionsOption = mock()
    private val clearCacheUseCase: ClearCacheUseCase = mock()
    private val getCacheSizeUseCase: GetCacheSizeUseCase = mock()
    private val getOfflineFolderSizeUseCase: GetOfflineFolderSizeUseCase = mock()
    private val clearOfflineUseCase: ClearOfflineUseCase = mock()
    private val removeAllVersionsUseCase: RemoveAllVersionsUseCase = mock()
    private val isRubbishBinAutopurgeEnabledUseCase: IsRubbishBinAutopurgeEnabledUseCase = mock()
    private val getRubbishBinAutopurgePeriodUseCase: GetRubbishBinAutopurgePeriodUseCase = mock()
    private val setRubbishBinAutopurgePeriodUseCase: SetRubbishBinAutopurgePeriodUseCase = mock()
    private val isRubbishBinAutopurgePeriodValidUseCase: IsRubbishBinAutopurgePeriodValidUseCase =
        mock()
    private val myAccountUpdateFlow = MutableStateFlow(
        MyAccountUpdate(
            MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
        )
    )
    private val monitorMyAccountUpdateUseCase = mock<MonitorMyAccountUpdateUseCase>() {
        on { invoke() }.thenReturn(myAccountUpdateFlow)
    }

    @BeforeEach
    fun setUp() {
        initViewModel()
    }

    @AfterEach
    fun reset() {
        reset(
            getFolderVersionInfo,
            monitorConnectivityUseCase,
            isConnectedToInternetUseCase,
            getFileVersionsOption,
            monitorUserUpdates,
            enableFileVersionsOption,
            clearCacheUseCase,
            getCacheSizeUseCase,
            getOfflineFolderSizeUseCase,
            clearOfflineUseCase,
            removeAllVersionsUseCase,
            isRubbishBinAutopurgeEnabledUseCase,
            getRubbishBinAutopurgePeriodUseCase,
            setRubbishBinAutopurgePeriodUseCase,
            isRubbishBinAutopurgePeriodValidUseCase
        )
    }

    private fun initViewModel() {
        underTest = FilePreferencesViewModel(
            getFolderVersionInfo,
            monitorConnectivityUseCase,
            getFileVersionsOption,
            monitorUserUpdates,
            enableFileVersionsOption,
            isConnectedToInternetUseCase,
            clearCacheUseCase,
            getCacheSizeUseCase,
            getOfflineFolderSizeUseCase,
            clearOfflineUseCase,
            removeAllVersionsUseCase,
            isRubbishBinAutopurgeEnabledUseCase,
            getRubbishBinAutopurgePeriodUseCase,
            setRubbishBinAutopurgePeriodUseCase,
            isRubbishBinAutopurgePeriodValidUseCase,
            monitorMyAccountUpdateUseCase
        )
    }

    @Test
    fun `test that isFileVersioningEnabled is true if getFileVersionsOption returns false`() =
        runTest {
            whenever(getFileVersionsOption(true)).thenReturn(false)
            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertTrue(state.isFileVersioningEnabled)
            }
        }

    @Test
    fun `test that isFileVersioningEnabled is false if getFileVersionsOption returns true`() =
        runTest {
            whenever(getFileVersionsOption(true)).thenReturn(true)
            initViewModel()
            underTest.state.test {
                awaitItem()
                val state = awaitItem()
                assertFalse(state.isFileVersioningEnabled)
            }
        }

    @Test
    fun `test that isFileVersioningEnabled is false if monitorUserUpdates emit DisableVersions and getFileVersionsOption returns true`() =
        runTest {
            whenever(getFileVersionsOption(true)).thenReturn(true)
            fakeMonitorUserUpdates.emit(UserChanges.DisableVersions)
            underTest.state.test {
                awaitItem()
                val state = awaitItem()
                assertFalse(state.isFileVersioningEnabled)
            }
        }

    @Test
    fun `test that isFileVersioningEnabled is true if monitorUserUpdates emit DisableVersions and getFileVersionsOption returns false`() =
        runTest {
            whenever(getFileVersionsOption(true)).thenReturn(false)
            fakeMonitorUserUpdates.emit(UserChanges.DisableVersions)
            underTest.state.test {
                val state = awaitItem()
                assertTrue(state.isFileVersioningEnabled)
            }
        }

    @Test
    fun `test that isFileVersioningEnabled is true if calling enableFileVersionOption returns true`() =
        runTest {
            underTest.enableFileVersionOption(true)
            underTest.state.test {
                val state = awaitItem()
                assertTrue(state.isFileVersioningEnabled)
            }
        }

    @Test
    fun `test that isFileVersioningEnabled is false if calling enableFileVersionOption returns false`() =
        runTest {
            underTest.enableFileVersionOption(false)
            underTest.state.test {
                awaitItem()
                val state = awaitItem()
                assertFalse(state.isFileVersioningEnabled)
            }
        }

    @Test
    fun `test that exception when enable file version option is not propagated`() = runTest {
        whenever(enableFileVersionsOption(false))
            .thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            state.map { it.isFileVersioningEnabled }.test {
                enableFileVersionOption(false)
                assertTrue(awaitItem())
            }
        }
    }

    @Test
    fun `test that updateOfflineSize is not null value when getOfflineFolderSize is invoked`() =
        runTest {
            val size = 1234L
            whenever(getOfflineFolderSizeUseCase()).thenReturn(size)
            underTest.getOfflineFolderSize()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                val result = awaitItem()
                assertThat(result.updateOfflineSize).isEqualTo(size)
            }
        }

    @Test
    fun `test that updateOfflineSize is null value when resetUpdateOfflineSize is invoked`() =
        runTest {
            val size = 1234L
            whenever(getOfflineFolderSizeUseCase()).thenReturn(size)
            underTest.getOfflineFolderSize()
            testScheduler.advanceUntilIdle()
            underTest.resetUpdateOfflineSize()
            underTest.state.test {
                val result = awaitItem()
                assertThat(result.updateOfflineSize).isNull()
            }
        }

    @Test
    fun `test that clearAllVersions triggers deleteAllVersionsEvent on success`() = runTest {
        whenever(removeAllVersionsUseCase()).thenReturn(Unit)
        underTest.clearAllVersions()
        advanceUntilIdle()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.deleteAllVersionsEvent).isNotNull()
            assertThat((state.deleteAllVersionsEvent as StateEventWithContentTriggered<Throwable?>).content).isNull()
        }
    }

    @Test
    fun `test that clearAllVersions triggers deleteAllVersionsEvent with error on failure`() =
        runTest {
            val exception = RuntimeException()
            whenever(removeAllVersionsUseCase()).thenThrow(exception)

            underTest.clearAllVersions()
            advanceUntilIdle()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.deleteAllVersionsEvent).isNotNull()
                assertThat((state.deleteAllVersionsEvent as StateEventWithContentTriggered<Throwable?>).content).isEqualTo(
                    exception
                )
            }
        }

    @Test
    fun `test that resetDeleteAllVersionsEvent sets deleteAllVersionsEvent to consumed`() =
        runTest {
            underTest.resetDeleteAllVersionsEvent()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.deleteAllVersionsEvent).isEqualTo(consumed())
            }
        }

    @ParameterizedTest(name = "returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isRubbishBinAutopurgePeriodValid`(isValid: Boolean) = runTest {
        whenever(isRubbishBinAutopurgePeriodValidUseCase(7)).thenReturn(isValid)
        assertThat(underTest.isRubbishBinAutopurgePeriodValid(7)).isEqualTo(isValid)
    }

    @Test
    fun `test that setRubbishBinAutopurgePeriod triggers rubbishBinAutopurgePeriod on success`() =
        runTest {
            whenever(setRubbishBinAutopurgePeriodUseCase(7)).thenReturn(Unit)
            underTest.setRubbishBinAutopurgePeriod(7)
            advanceUntilIdle()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.rubbishBinAutopurgePeriod).isEqualTo(7)
            }
        }

    @Test
    fun `test that monitor my account update emits the correct value`() = runTest {
        val myAccountUpdate = MyAccountUpdate(MyAccountUpdate.Action.UPDATE_ACCOUNT_DETAILS)

        myAccountUpdateFlow.emit(myAccountUpdate)
        advanceUntilIdle()
        underTest.monitorMyAccountUpdateEvent.test {
            assertThat(awaitItem()).isEqualTo(myAccountUpdate)
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
