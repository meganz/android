package test.mega.privacy.android.app.presentation.manager

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.manager.UserInfoViewModel
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.MonitorContactCacheUpdates
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.UpdateMyAvatarWithNewEmail
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.contact.ReloadContactDatabase
import mega.privacy.android.domain.usecase.login.CheckPasswordReminderUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserInfoViewModelTest {
    private lateinit var underTest: UserInfoViewModel
    private val getCurrentUserFullName: GetCurrentUserFullName = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()
    private val updateMyAvatarWithNewEmail: UpdateMyAvatarWithNewEmail = mock()
    private val fakeMonitorUserUpdates = MutableSharedFlow<UserChanges>()
    private val fakeMonitorOtherUsersUpdates = MutableSharedFlow<UserUpdate>()
    private val monitorUserUpdates: MonitorUserUpdates = mock {
        on { invoke() }.thenReturn(fakeMonitorUserUpdates)
    }
    private val monitorContactCacheUpdates: MonitorContactCacheUpdates = mock {
        on { invoke() }.thenReturn(fakeMonitorOtherUsersUpdates)
    }
    private val context: Context = mock()
    private val reloadContactDatabase: ReloadContactDatabase = mock()
    private val applicationScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    private val avatarContentMapper: AvatarContentMapper = mock()
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase = mock {
        onBlocking { invoke() }.thenReturn(1)
    }
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase = mock()
    private val monitorMyAvatarFile: MonitorMyAvatarFile = mock()
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase = mock()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = UserInfoViewModel(
            getCurrentUserFullName = getCurrentUserFullName,
            getCurrentUserEmail = getCurrentUserEmail,
            monitorUserUpdates = monitorUserUpdates,
            updateMyAvatarWithNewEmail = updateMyAvatarWithNewEmail,
            monitorContactCacheUpdates = monitorContactCacheUpdates,
            context = context,
            reloadContactDatabase = reloadContactDatabase,
            applicationScope = applicationScope,
            avatarContentMapper = avatarContentMapper,
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            checkPasswordReminderUseCase = checkPasswordReminderUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        wheneverBlocking { getMyAvatarColorUseCase() }.thenReturn(1)
        reset(
            getCurrentUserFullName,
            getCurrentUserEmail,
            updateMyAvatarWithNewEmail,
            context,
            reloadContactDatabase,
            getMyAvatarFileUseCase,
            monitorMyAvatarFile,
            checkPasswordReminderUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that email returns correctly when GetCurrentUserEmail returns the value`() = runTest {
        val expectedEmail = "myEmail"
        whenever(context.getString(any())).thenReturn("")
        whenever(getCurrentUserEmail()).thenReturn(expectedEmail)
        whenever(getCurrentUserFullName(any(), any(), any())).thenReturn("myName")
        underTest.getUserInfo()
        advanceUntilIdle()
        underTest.state.test {
            val item = awaitItem()
            assertEquals(expectedEmail, item.email)
        }
    }

    @Test
    fun `test that name returns correctly when GetCurrentUserFullName returns the value`() =
        runTest {
            val expectedName = "myName"
            whenever(context.getString(any())).thenReturn("")
            whenever(getCurrentUserFullName(true, "", "")).thenReturn(expectedName)
            underTest.getUserInfo()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                val item = awaitItem()
                assertEquals(expectedName, item.fullName)
            }
        }

    @Test
    fun `test that email update correctly when monitorUserUpdates emit UserChanges Email`() =
        runTest {
            val expectedEmail = "myEmail"
            whenever(context.getString(any())).thenReturn("")
            whenever(getCurrentUserEmail()).thenReturn(expectedEmail)
            whenever(getCurrentUserFullName(any(), any(), any())).thenReturn("myName")
            whenever(getMyAvatarColorUseCase()).thenReturn(1)
            underTest.getUserInfo()
            underTest.state.test {
                awaitItem()
                awaitItem()
                val item = awaitItem()
                assertEquals(expectedEmail, item.email)
            }
            val expectedNewEmail = "myNewEmail"
            whenever(getCurrentUserEmail()).thenReturn(expectedNewEmail)
            fakeMonitorUserUpdates.emit(UserChanges.Email)
            verify(updateMyAvatarWithNewEmail, times(1)).invoke(expectedEmail, expectedNewEmail)
            underTest.state.test {
                val item = awaitItem()
                assertEquals(expectedNewEmail, item.email)
            }
        }

    @Test
    fun `test that call to ReloadContactDatabase true when call refreshContactDatabase as true`() =
        runTest {
            underTest.refreshContactDatabase(true)
            verify(reloadContactDatabase, times(1)).invoke(true)
        }

    @Test
    fun `test that call to ReloadContactDatabase false when call refreshContactDatabase as false`() =
        runTest {
            underTest.refreshContactDatabase(false)
            verify(reloadContactDatabase, times(1)).invoke(false)
        }

    @Test
    fun `test that showTestPassword as true when call checkPasswordReminderUseCase returns true`() =
        runTest {
            whenever(checkPasswordReminderUseCase(false)).thenReturn(true)
            underTest.checkPasswordReminderStatus()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                Truth.assertThat(awaitItem().isTestPasswordRequired).isEqualTo(true)
            }
        }

    @Test
    fun `test that showTestPassword as false when call checkPasswordReminderUseCase returns false`() =
        runTest {
            whenever(checkPasswordReminderUseCase(false)).thenReturn(false)
            underTest.checkPasswordReminderStatus()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                Truth.assertThat(awaitItem().isTestPasswordRequired).isEqualTo(false)
            }
        }

    @Test
    fun `test that showTestPassword as false when call showTestPasswordHandled`() =
        runTest {
            underTest.onTestPasswordHandled()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                Truth.assertThat(awaitItem().isTestPasswordRequired).isEqualTo(false)
            }
        }
}