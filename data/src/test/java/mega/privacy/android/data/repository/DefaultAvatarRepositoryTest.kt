package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.BitmapFactoryWrapper
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUser
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
internal class DefaultAvatarRepositoryTest {
    companion object {
        private const val CURRENT_USER_HANDLE = 123L
        private const val CURRENT_USER_EMAIL = "myemail"
    }

    private lateinit var underTest: DefaultAvatarRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val cacheFolderGateway = mock<CacheFolderGateway>()
    private val contactsRepository = mock<ContactsRepository>()
    private val currentUser = mock<MegaUser> {
        on { it.isOwnChange }.thenReturn(0)
        on { it.hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong()) }.thenReturn(true)
        on { it.handle }.thenReturn(CURRENT_USER_HANDLE)
        on { it.email }.thenReturn(CURRENT_USER_EMAIL)
    }
    private val sharedFlow = MutableSharedFlow<GlobalUpdate>()
    private val avatarWrapper = mock<AvatarWrapper>()
    private val bitmapFactoryWrapper = mock<BitmapFactoryWrapper>()
    private val databaseHandler = mock<DatabaseHandler>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        whenever(cacheFolderGateway.buildAvatarFile(any())).thenReturn(File(""))
        whenever(megaApiGateway.globalUpdates).thenReturn(sharedFlow)
        underTest = DefaultAvatarRepository(
            megaApiGateway = megaApiGateway,
            avatarWrapper = avatarWrapper,
            bitmapFactoryWrapper = bitmapFactoryWrapper,
            cacheFolderGateway = cacheFolderGateway,
            contactsRepository = contactsRepository,
            sharingScope = TestScope(),
            ioDispatcher = UnconfinedTestDispatcher(),
            databaseHandler = databaseHandler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when globalUpdates emit OnAccountUpdate then monitorMyAvatarFile not emit`() = runTest {
        whenever(megaApiGateway.getLoggedInUser()).thenReturn(currentUser)
        sharedFlow.emit(GlobalUpdate.OnAccountUpdate)
        val collectJob = launch(UnconfinedTestDispatcher()) {
            underTest.monitorMyAvatarFile().collect {
                fail("monitorMyAvatarFile emit")
            }
        }
        delay(300L)
        collectJob.cancel()
    }

    @Test
    fun `when globalUpdates emit OnUsersUpdate with current user then monitorMyAvatarFile emit avatar file`() =
        runTest {
            testScheduler.advanceUntilIdle()
            whenever(megaApiGateway.myUser).thenReturn(currentUser)
            whenever(megaApiGateway.getUserAvatar(any(), any())).thenReturn(true)
            underTest.monitorMyAvatarFile().test {
                sharedFlow.emit(GlobalUpdate.OnUsersUpdate(users = arrayListOf(currentUser)))
                val value = awaitItem()
                assertTrue(value != null)
            }
        }

    @Test
    fun `when globalUpdates emit OnUsersUpdate with another user then monitorMyAvatarFile not emit`() =
        runTest {
            whenever(megaApiGateway.myUser).thenReturn(currentUser)
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest.monitorMyAvatarFile().collect {
                    fail("monitorMyAvatarFile emit")
                }
            }
            val anotherUser = mock<MegaUser> {
                on { it.isOwnChange }.thenReturn(0)
                on { it.hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong()) }.thenReturn(true)
                on { it.email }.thenReturn("anotherUser")
            }
            sharedFlow.emit(GlobalUpdate.OnUsersUpdate(users = arrayListOf(anotherUser)))
            whenever(megaApiGateway.getUserAvatar(any(), any())).thenReturn(true)
            delay(300L)
            collectJob.cancel()
        }

    @Test
    fun `when globalUpdates emit OnUsersUpdate with current user and isOwnChange more than 0 then monitorMyAvatarFile not emit`() =
        runTest {
            whenever(megaApiGateway.myUser).thenReturn(currentUser)
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest.monitorMyAvatarFile().collect {
                    fail("monitorMyAvatarFile emit")
                }
            }
            whenever(currentUser.isOwnChange).thenReturn(1)
            sharedFlow.emit(GlobalUpdate.OnUsersUpdate(users = arrayListOf(currentUser)))
            whenever(megaApiGateway.getUserAvatar(any(), any())).thenReturn(true)
            delay(300L)
            collectJob.cancel()
        }

    @Test
    fun `test that updateMyAvatarWithNewEmail return true when cacheFolderGateway returns the oldFile`() =
        runTest {
            val oldEmail = "oldEmail"
            val newEmail = "newEmail"
            val oldFile = mock<File> {
                on { exists() }.thenReturn(true)
            }
            val newFile = mock<File>()
            whenever(cacheFolderGateway.buildAvatarFile(oldEmail + FileConstant.JPG_EXTENSION))
                .thenReturn(oldFile)
            whenever(cacheFolderGateway.buildAvatarFile(newEmail + FileConstant.JPG_EXTENSION))
                .thenReturn(newFile)
            whenever(oldFile.renameTo(newFile)).thenReturn(true)
            assertTrue(underTest.updateMyAvatarWithNewEmail(oldEmail, newEmail))
        }

    @Test
    fun `test that updateMyAvatarWithNewEmail return true when cacheFolderGateway can not returns the oldFile`() =
        runTest {
            val oldEmail = "oldEmail"
            val newEmail = "newEmail"
            val oldFile = mock<File> {
                on { exists() }.thenReturn(false)
            }
            val newFile = mock<File>()
            whenever(cacheFolderGateway.buildAvatarFile(oldEmail + FileConstant.JPG_EXTENSION))
                .thenReturn(oldFile)
            whenever(cacheFolderGateway.buildAvatarFile(newEmail + FileConstant.JPG_EXTENSION))
                .thenReturn(newFile)
            whenever(oldFile.renameTo(newFile)).thenReturn(false)
            assertFalse(underTest.updateMyAvatarWithNewEmail(oldEmail, newEmail))
        }

    @Test
    fun `test that setAvatar calls success value when calling API returns success`() =
        runTest {
            whenever(megaApiGateway.setAvatar(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = mock(),
                    error = mock {
                        on { errorCode }.thenReturn(
                            MegaError.API_OK
                        )
                    },
                )
            }
            underTest.setAvatar(filePath = "")
        }

    @Test(expected = MegaException::class)
    fun `test that setAvatar throw exception success value when calling API returns failed`() =
        runTest {
            whenever(megaApiGateway.setAvatar(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = mock(),
                    error = mock { on { errorCode }.thenReturn(MegaError.API_EARGS) },
                )
            }
            underTest.setAvatar(filePath = "")
        }

    @Test
    fun `test that getMyAvatarFile from the cache when pass isForceRefresh as false`() = runTest {
        val email = "my_email"
        whenever(databaseHandler.myEmail).thenReturn(email)
        underTest.getMyAvatarFile(isForceRefresh = false)
        verify(megaApiGateway, times(0)).myUser
        verify(megaApiGateway, times(0)).getUserAvatar(any(), any())
        verify(
            cacheFolderGateway,
            times(1)
        ).buildAvatarFile(email + FileConstant.JPG_EXTENSION)
    }

    @Test
    fun `test that getMyAvatarFile from the sdk when pass isForceRefresh as true`() = runTest {
        val expectedFile = mock<File> {
            on { absolutePath }.thenReturn("path")
        }
        whenever(megaApiGateway.myUser).thenReturn(currentUser)
        whenever(megaApiGateway.accountEmail).thenReturn(CURRENT_USER_EMAIL)
        whenever(cacheFolderGateway.buildAvatarFile(CURRENT_USER_EMAIL + FileConstant.JPG_EXTENSION)).thenReturn(expectedFile)
        Truth.assertThat(underTest.getMyAvatarFile(true)).isEqualTo(expectedFile)
    }
}