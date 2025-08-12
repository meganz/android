package mega.privacy.android.app.myAccount.editProfile

import android.graphics.Color
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.withCoroutineExceptions
import mega.privacy.android.app.presentation.editProfile.EditProfileViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserFirstName
import mega.privacy.android.domain.usecase.contact.GetCurrentUserLastName
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertEquals

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EditProfileViewModelTest {

    private val getMyAvatarColorUseCase = mock<GetMyAvatarColorUseCase>()
    private val getMyAvatarFileUseCase = mock<GetMyAvatarFileUseCase>()
    private val hasOfflineFilesUseCase = mock<HasOfflineFilesUseCase>()
    private val ongoingTransfersExistUseCase = mock<OngoingTransfersExistUseCase>()
    private val monitorMyAvatarFile = mock<MonitorMyAvatarFile>()
    private val colorMyAvatar = Color.RED
    private val monitorMyAvatarFileFlow = MutableSharedFlow<File?>()
    private val firstName = "FirstName"
    private val getCurrentUserFirstName: GetCurrentUserFirstName = mock()
    private val lastName = "LastName"
    private val getCurrentUserLastName: GetCurrentUserLastName = mock()
    private lateinit var userUpdates: Channel<UserChanges>
    private val monitorUserUpdates: MonitorUserUpdates = mock()

    @BeforeEach
    fun setUp() {
        whenever(monitorMyAvatarFile()).thenReturn(monitorMyAvatarFileFlow)
        getMyAvatarColorUseCase.stub {
            onBlocking { invoke() }.doReturn(colorMyAvatar)
        }
        getMyAvatarFileUseCase.stub {
            onBlocking { invoke(any()) }.doReturn(null)
        }
        // Set up mocks for the use cases called in init
        hasOfflineFilesUseCase.stub {
            onBlocking { invoke() }.doReturn(false)
        }
        ongoingTransfersExistUseCase.stub {
            onBlocking { invoke() }.doReturn(false)
        }
        // Set up monitorUserUpdates mock
        userUpdates = Channel()
        whenever(monitorUserUpdates()).thenReturn(userUpdates.consumeAsFlow())
    }

    private fun createViewModel(
        firstNameValue: String? = firstName,
        lastNameValue: String? = lastName,
    ): EditProfileViewModel {
        // Create fresh mocks for each test to avoid conflicts
        val testGetCurrentUserFirstName = mock<GetCurrentUserFirstName>()
        val testGetCurrentUserLastName = mock<GetCurrentUserLastName>()

        // Set up the test mocks
        testGetCurrentUserFirstName.stub {
            onBlocking { invoke(false) }.doReturn(firstNameValue ?: "")
        }
        testGetCurrentUserLastName.stub {
            onBlocking { invoke(false) }.doReturn(lastNameValue ?: "")
        }

        return EditProfileViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getCurrentUserFirstName = testGetCurrentUserFirstName,
            getCurrentUserLastName = testGetCurrentUserLastName,
            monitorUserUpdates = monitorUserUpdates,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase
        )
    }

    @Test
    fun `when monitorMyAvatarFile emit new file then avatarFile is null`() =
        runTest {
            val testViewModel = createViewModel()
            monitorMyAvatarFileFlow.emit(null)
            testViewModel.state.test {
                val state = awaitItem()
                assertEquals(state.avatarColor, colorMyAvatar)
                assertEquals(state.avatarFile, null)
            }
        }

    @Test
    fun `when monitorMyAvatarFile emit new file then avatarFile is not null`() =
        runTest {
            val testViewModel = createViewModel()
            val file = mock<File>()
            monitorMyAvatarFileFlow.emit(file)
            testViewModel.state.test {
                val state = awaitItem()
                assertEquals(state.avatarColor, colorMyAvatar)
                assertEquals(state.avatarFile, file)
            }
        }

    @Test
    internal fun `test that an exception on get last name is not propagated`() =
        withCoroutineExceptions {
            runTest {
                val testViewModel = createViewModel()
                // Wait for initial state to be set
                testViewModel.state.test {
                    val initialState = awaitItem()
                    // Now set up the exception scenario
                    getCurrentUserLastName.stub {
                        onBlocking { invoke(any()) }.thenAnswer {
                            throw MegaException(
                                1,
                                "Get last name threw an exception"
                            )
                        }
                    }
                    userUpdates.send(UserChanges.Lastname)
                    // Wait for the next state update or timeout
                    awaitItem()
                }
            }
        }

    @Test
    internal fun `test that an exception on get first name is not propagated`() =
        withCoroutineExceptions {
            runTest {
                val testViewModel = createViewModel()
                // Wait for initial state to be set
                testViewModel.state.test {
                    val initialState = awaitItem()
                    // Now set up the exception scenario
                    getCurrentUserFirstName.stub {
                        onBlocking { invoke(true) }.thenAnswer {
                            throw MegaException(
                                1,
                                "Get first name threw an exception"
                            )
                        }
                    }
                    userUpdates.send(UserChanges.Firstname)
                    // Wait for the next state update or timeout
                    awaitItem()
                }
            }
        }

    @Test
    internal fun `test that offlineFilesExist is set correctly`() = runTest {
        val testViewModel = createViewModel()
        whenever(hasOfflineFilesUseCase()).thenReturn(true)

        testViewModel.checkOfflineFiles()

        testViewModel.state.test {
            assertThat(awaitItem().offlineFilesExist).isTrue()
        }
    }

    @Test
    internal fun `test that transfersExist is set correctly`() = runTest {
        val testViewModel = createViewModel()
        whenever(ongoingTransfersExistUseCase()).thenReturn(true)

        testViewModel.checkOngoingTransfers()

        testViewModel.state.test {
            assertThat(awaitItem().transfersExist).isTrue()
        }
    }

    @Test
    internal fun `test that first name returns correctly from use case`() = runTest {
        val expectedFirstName = "John"
        // Set up the mock before creating the ViewModel
        whenever(getCurrentUserFirstName(false)).thenReturn(expectedFirstName)

        // Create a new ViewModel instance with the updated mock
        val testViewModel = EditProfileViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getCurrentUserFirstName = getCurrentUserFirstName,
            getCurrentUserLastName = getCurrentUserLastName,
            monitorUserUpdates = monitorUserUpdates,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase
        )

        val result = testViewModel.getFirstName()

        assertThat(result).isEqualTo(expectedFirstName)
    }

    @Test
    internal fun `test that last name returns correctly from use case`() = runTest {
        val expectedLastName = "Doe"

        // Create a new ViewModel instance with the updated mock
        val testViewModel = createViewModel(lastNameValue = expectedLastName)

        val result = testViewModel.getLastName()

        assertThat(result).isEqualTo(expectedLastName)
    }

    @Test
    internal fun `test that first name handles empty string correctly`() = runTest {
        // Set up the mock before creating the ViewModel
        whenever(getCurrentUserFirstName(false)).thenReturn("")

        // Create a new ViewModel instance with the updated mock
        val testViewModel = EditProfileViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getCurrentUserFirstName = getCurrentUserFirstName,
            getCurrentUserLastName = getCurrentUserLastName,
            monitorUserUpdates = monitorUserUpdates,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase
        )

        val result = testViewModel.getFirstName()

        assertThat(result).isEmpty()
    }

    @Test
    internal fun `test that last name handles empty string correctly`() = runTest {
        // Create a new ViewModel instance with the updated mock
        val testViewModel = createViewModel(lastNameValue = "")

        val result = testViewModel.getLastName()

        assertThat(result).isEmpty()
    }

    @Test
    internal fun `test that first name handles null correctly`() = runTest {
        // Set up the mock before creating the ViewModel
        whenever(getCurrentUserFirstName(false)).thenReturn(null)

        // Create a new ViewModel instance with the updated mock
        val testViewModel = EditProfileViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getCurrentUserFirstName = getCurrentUserFirstName,
            getCurrentUserLastName = getCurrentUserLastName,
            monitorUserUpdates = monitorUserUpdates,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase
        )

        val result = testViewModel.getFirstName()

        assertThat(result).isEmpty()
    }

    @Test
    internal fun `test that last name handles null correctly`() = runTest {
        // Create a new ViewModel instance with null value
        val testViewModel = createViewModel(lastNameValue = null)

        val result = testViewModel.getLastName()

        assertThat(result).isEmpty()
    }

    @Test
    internal fun `test that first name with 40 characters is handled correctly`() = runTest {
        val firstName40Chars = "a".repeat(40)
        // Set up the mock before creating the ViewModel
        whenever(getCurrentUserFirstName(false)).thenReturn(firstName40Chars)

        // Create a new ViewModel instance with the updated mock
        val testViewModel = EditProfileViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getCurrentUserFirstName = getCurrentUserFirstName,
            getCurrentUserLastName = getCurrentUserLastName,
            monitorUserUpdates = monitorUserUpdates,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase
        )

        val result = testViewModel.getFirstName()

        assertThat(result).isEqualTo(firstName40Chars)
        assertThat(result.length).isEqualTo(40)
    }

    @Test
    internal fun `test that last name with 40 characters is handled correctly`() = runTest {
        val lastName40Chars = "b".repeat(40)

        // Create a new ViewModel instance with the updated mock
        val testViewModel = createViewModel(lastNameValue = lastName40Chars)

        val result = testViewModel.getLastName()

        assertThat(result).isEqualTo(lastName40Chars)
        assertThat(result.length).isEqualTo(40)
    }

    @Test
    internal fun `test that first name with unicode characters is handled correctly`() = runTest {
        val unicodeFirstName = "José-María"
        // Set up the mock before creating the ViewModel
        whenever(getCurrentUserFirstName(false)).thenReturn(unicodeFirstName)

        // Create a new ViewModel instance with the updated mock
        val testViewModel = EditProfileViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getCurrentUserFirstName = getCurrentUserFirstName,
            getCurrentUserLastName = getCurrentUserLastName,
            monitorUserUpdates = monitorUserUpdates,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase
        )

        val result = testViewModel.getFirstName()

        assertThat(result).isEqualTo(unicodeFirstName)
    }

    @Test
    internal fun `test that last name with unicode characters is handled correctly`() = runTest {
        val unicodeLastName = "Müller-Schmidt"

        // Create a new ViewModel instance with the updated mock
        val testViewModel = createViewModel(lastNameValue = unicodeLastName)

        val result = testViewModel.getLastName()

        assertThat(result).isEqualTo(unicodeLastName)
    }
}

