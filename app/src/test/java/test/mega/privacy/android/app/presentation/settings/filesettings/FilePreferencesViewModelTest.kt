package test.mega.privacy.android.app.presentation.settings.filesettings

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.filesettings.FilePreferencesViewModel
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetFolderVersionInfo
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.EnableFileVersionsOption
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class FilePreferencesViewModelTest {
    private lateinit var underTest: FilePreferencesViewModel

    private val getFolderVersionInfo: GetFolderVersionInfo = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val getFileVersionsOption: GetFileVersionsOption = mock {
        on { runBlocking { invoke(any()) } }.thenReturn(false)
    }
    private val fakeMonitorUserUpdates = MutableSharedFlow<UserChanges>()
    private val monitorUserUpdates: MonitorUserUpdates = mock {
        on { invoke() }.thenReturn(fakeMonitorUserUpdates)
    }
    private val enableFileVersionsOption: EnableFileVersionsOption = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        initViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        underTest = FilePreferencesViewModel(
            getFolderVersionInfo,
            monitorConnectivityUseCase,
            getFileVersionsOption,
            monitorUserUpdates,
            enableFileVersionsOption
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
}