package test.mega.privacy.android.app.myAccount.editProfile

import android.graphics.Color
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.editProfile.EditProfileViewModel
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.contact.GetCurrentUserFirstName
import mega.privacy.android.domain.usecase.contact.GetCurrentUserLastName
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.extensions.withCoroutineExceptions
import java.io.File
import kotlin.test.assertEquals


@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EditProfileViewModelTest {
    private lateinit var underTest: EditProfileViewModel

    private val getMyAvatarColorUseCase = mock<GetMyAvatarColorUseCase>()
    private val getMyAvatarFileUseCase = mock<GetMyAvatarFileUseCase>()
    private val monitorMyAvatarFile = mock<MonitorMyAvatarFile>()
    private val colorMyAvatar = Color.RED
    private val monitorMyAvatarFileFlow = MutableSharedFlow<File?>()
    private val firstName = "FirstName"
    private val getCurrentUserFirstName: GetCurrentUserFirstName =
        mock { onBlocking { invoke(false) }.thenReturn(firstName) }
    private val lastName = "LastName"
    private val getCurrentUserLastName: GetCurrentUserLastName =
        mock { onBlocking { invoke(false) }.thenReturn(lastName) }
    private lateinit var userUpdates: Channel<UserChanges>
    private val monitorUserUpdates: MonitorUserUpdates =
        mock {
            on { invoke() }.thenAnswer {
                userUpdates = Channel()
                userUpdates.consumeAsFlow()
            }
        }


    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun setUp() {

        whenever(monitorMyAvatarFile()).thenReturn(monitorMyAvatarFileFlow)
        getMyAvatarColorUseCase.stub {
            onBlocking { invoke() }.doReturn(colorMyAvatar)
        }
        underTest = EditProfileViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getCurrentUserFirstName = getCurrentUserFirstName,
            getCurrentUserLastName = getCurrentUserLastName,
            monitorUserUpdates = monitorUserUpdates,
        )
    }

    @AfterAll
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

    @Test
    internal fun `test that an exception on get last name is not propagated`() =
        withCoroutineExceptions {
            runTest {
                getCurrentUserLastName.stub {
                    onBlocking { invoke(any()) }.thenAnswer {
                        throw MegaException(
                            1,
                            "Get last name threw an exception"
                        )
                    }
                }

                underTest.state.test {
                    assertThat(awaitItem().lastName).isEqualTo(lastName)
                    userUpdates.send(UserChanges.Lastname)
                }
            }
        }

    @Test
    internal fun `test that an exception on get first name is not propagated`() =
        withCoroutineExceptions {
            runTest {
                getCurrentUserFirstName.stub {
                    onBlocking { invoke(true) }.thenAnswer {
                        throw MegaException(
                            1,
                            "Get first name threw an exception"
                        )
                    }
                }

                underTest.state.test {
                    assertThat(awaitItem().firstName).isEqualTo(firstName)
                    userUpdates.send(UserChanges.Firstname)
                }
            }
        }
}

