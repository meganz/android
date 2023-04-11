package test.mega.privacy.android.app.myAccount.editProfile

import android.graphics.Color
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.editProfile.EditProfileViewModel
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.contact.GetCurrentUserFirstName
import mega.privacy.android.domain.usecase.contact.GetCurrentUserLastName
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertEquals


@ExperimentalCoroutinesApi
internal class EditProfileViewModelTest {
    private lateinit var underTest: EditProfileViewModel

    private val getMyAvatarColorUseCase = mock<GetMyAvatarColorUseCase>()
    private val getMyAvatarFile = mock<GetMyAvatarFile>()
    private val monitorMyAvatarFile = mock<MonitorMyAvatarFile>()
    private val colorMyAvatar = Color.RED
    private val monitorMyAvatarFileFlow = MutableSharedFlow<File?>()
    private val getCurrentUserFirstName: GetCurrentUserFirstName = mock()
    private val getCurrentUserLastName: GetCurrentUserLastName = mock()
    private val monitorUserUpdates: MonitorUserUpdates = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        whenever(monitorMyAvatarFile()).thenReturn(monitorMyAvatarFileFlow)
        getMyAvatarColorUseCase.stub {
            onBlocking { invoke() }.doReturn(colorMyAvatar)
        }
        underTest = EditProfileViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFile = getMyAvatarFile,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getCurrentUserFirstName = getCurrentUserFirstName,
            getCurrentUserLastName = getCurrentUserLastName,
            monitorUserUpdates = monitorUserUpdates,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when monitorMyAvatarFile emit new file then avatarFile is null`() =
        runTest {
            monitorMyAvatarFileFlow.emit(null)
            underTest.state.test {
                val state = awaitItem()
                assertEquals(state.avatarColor, colorMyAvatar)
                assertEquals(state.avatarFile, null)
            }
        }

    @Test
    fun `when monitorMyAvatarFile emit new file then avatarFile is not null`() =
        runTest {
            val file = mock<File>()
            monitorMyAvatarFileFlow.emit(file)
            underTest.state.test {
                val state = awaitItem()
                assertEquals(state.avatarColor, colorMyAvatar)
                assertEquals(state.avatarFile, file)
            }
        }
}