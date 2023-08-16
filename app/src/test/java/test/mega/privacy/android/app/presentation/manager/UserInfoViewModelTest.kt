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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.manager.UserInfoViewModel
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.UpdateMyAvatarWithNewEmail
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetCurrentUserAliases
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import mega.privacy.android.domain.usecase.contact.GetUserLastName
import mega.privacy.android.domain.usecase.contact.ReloadContactDatabase
import mega.privacy.android.domain.usecase.login.CheckPasswordReminderUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val monitorContactUpdates: MonitorContactUpdates = mock {
        on { invoke() }.thenReturn(fakeMonitorOtherUsersUpdates)
    }
    private val getUserFirstName: GetUserFirstName = mock()
    private val getUserLastName: GetUserLastName = mock()
    private val getCurrentUserAliases: GetCurrentUserAliases = mock()
    private val context: Context = mock()
    private val reloadContactDatabase: ReloadContactDatabase = mock()
    private val getContactEmail: GetContactEmail = mock()
    private val applicationScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    private val avatarContentMapper: AvatarContentMapper = mock()
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase = mock()
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase = mock()
    private val monitorMyAvatarFile: MonitorMyAvatarFile = mock()
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase = mock()

    @Before
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
            getUserFirstName = getUserFirstName,
            getUserLastName = getUserLastName,
            monitorContactUpdates = monitorContactUpdates,
            getCurrentUserAliases = getCurrentUserAliases,
            context = context,
            reloadContactDatabase = reloadContactDatabase,
            getContactEmail = getContactEmail,
            applicationScope = applicationScope,
            avatarContentMapper = avatarContentMapper,
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            checkPasswordReminderUseCase = checkPasswordReminderUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that email returns correctly when GetCurrentUserEmail returns the value`() = runTest {
        val expectedEmail = "myEmail"
        whenever(context.getString(any())).thenReturn("")
        whenever(getCurrentUserEmail()).thenReturn(expectedEmail)
        underTest.getUserInfo()
        testScheduler.advanceUntilIdle()
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
            underTest.getUserInfo()
            underTest.state.test {
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
    fun `test that call to getUserFirstName and getUserLastName use case when monitorOtherUsersUpdates emit Firstname and Lastname`() =
        runTest {
            initViewModelState()
            testScheduler.advanceUntilIdle()
            val userId = UserId(123L)
            val map = mapOf(userId to listOf(UserChanges.Firstname, UserChanges.Lastname))
            fakeMonitorOtherUsersUpdates.emit(UserUpdate(map))
            verify(getUserFirstName, times(1)).invoke(
                handle = userId.id,
                skipCache = true,
                shouldNotify = true
            )
            verify(getUserLastName, times(1)).invoke(
                handle = userId.id,
                skipCache = true,
                shouldNotify = true
            )
        }

    @Test
    fun `test that call to getUserFirstName use case when monitorOtherUsersUpdates emit Firstname`() =
        runTest {
            initViewModelState()
            testScheduler.advanceUntilIdle()
            val userId = UserId(123L)
            val map = mapOf(userId to listOf(UserChanges.Firstname))
            fakeMonitorOtherUsersUpdates.emit(UserUpdate(map))
            verify(getUserFirstName, times(1)).invoke(
                handle = userId.id,
                skipCache = true,
                shouldNotify = true
            )
            verifyNoInteractions(getUserLastName)
        }

    @Test
    fun `test that call to getUserLastName use case when monitorOtherUsersUpdates emit LastName`() =
        runTest {
            initViewModelState()
            testScheduler.advanceUntilIdle()
            val userId = UserId(123L)
            val map = mapOf(userId to listOf(UserChanges.Lastname))
            fakeMonitorOtherUsersUpdates.emit(UserUpdate(map))
            verify(getUserLastName, times(1)).invoke(
                handle = userId.id,
                skipCache = true,
                shouldNotify = true
            )
            verifyNoInteractions(getUserFirstName)
            verifyNoInteractions(getContactEmail)
            verifyNoInteractions(getCurrentUserAliases)
        }

    @Test
    fun `test that call to GetCurrentUserAliases use case when monitorOtherUsersUpdates emit Alias`() =
        runTest {
            initViewModelState()
            testScheduler.advanceUntilIdle()
            val userId = UserId(123L)
            val map = mapOf(userId to listOf(UserChanges.Alias))
            fakeMonitorOtherUsersUpdates.emit(UserUpdate(map))
            verify(getCurrentUserAliases, times(1)).invoke()
            verifyNoInteractions(getUserFirstName)
            verifyNoInteractions(getUserLastName)
            verifyNoInteractions(getContactEmail)
        }

    @Test
    fun `test that call to getContactEmail use case when monitorOtherUsersUpdates emit Email`() =
        runTest {
            initViewModelState()
            testScheduler.advanceUntilIdle()
            val userId = UserId(123L)
            val map = mapOf(userId to listOf(UserChanges.Email))
            fakeMonitorOtherUsersUpdates.emit(UserUpdate(map))
            verify(getContactEmail, times(1)).invoke(userId.id)
            verifyNoInteractions(getUserFirstName)
            verifyNoInteractions(getUserLastName)
            verifyNoInteractions(getCurrentUserAliases)
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

    private suspend fun initViewModelState() {
        val expectedEmail = "myEmail"
        val expectedName = "myName"
        whenever(context.getString(any())).thenReturn("")
        whenever(getCurrentUserFullName(true, "", "")).thenReturn(expectedName)
        whenever(getCurrentUserEmail()).thenReturn(expectedEmail)
        underTest.getUserInfo()
    }
}