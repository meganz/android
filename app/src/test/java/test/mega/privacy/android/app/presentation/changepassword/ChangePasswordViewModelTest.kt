package test.mega.privacy.android.app.presentation.changepassword

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.changepassword.ChangePasswordViewModel
import mega.privacy.android.domain.usecase.MonitorConnectivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChangePasswordViewModelTest {
    private lateinit var underTest: ChangePasswordViewModel
    private val testFlow = MutableStateFlow(false)
    private val monitorConnectivity = mock<MonitorConnectivity> {
        onBlocking { invoke() }.thenReturn(testFlow)
    }

    @Before
    fun setup() {
        underTest = ChangePasswordViewModel(monitorConnectivity)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that isConnected true when MonitorConnectivity emit true`() = runTest {
        testFlow.emit(true)
        assertTrue(underTest.isConnected)
    }

    @Test
    fun `test that isConnected false when MonitorConnectivity emit false`() = runTest {
        testFlow.emit(false)
        assertFalse(underTest.isConnected)
    }
}