package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.BitmapFactoryWrapper
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUser
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
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
    private val cacheGateway = mock<CacheGateway>()
    private val currentUser = mock<MegaUser> {
        on { it.isOwnChange }.thenReturn(0)
        on { it.hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong()) }.thenReturn(true)
        on { it.handle }.thenReturn(CURRENT_USER_HANDLE)
        on { it.email }.thenReturn(CURRENT_USER_EMAIL)
    }
    private val sharedFlow = MutableSharedFlow<GlobalUpdate>()
    private val avatarWrapper = mock<AvatarWrapper>()
    private val bitmapFactoryWrapper = mock<BitmapFactoryWrapper>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        whenever(megaApiGateway.globalUpdates).thenReturn(sharedFlow)
        underTest = DefaultAvatarRepository(
            megaApiGateway = megaApiGateway,
            avatarWrapper = avatarWrapper,
            bitmapFactoryWrapper = bitmapFactoryWrapper,
            cacheGateway = cacheGateway,
            sharingScope = TestScope(),
            ioDispatcher = UnconfinedTestDispatcher(),
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
            val file = mock<File> {
                on { exists() }.thenReturn(true)
                on { absolutePath }.thenReturn("path/to/avatar.jpg")
            }
            whenever(cacheGateway.buildAvatarFile(any())).thenReturn(file)
            whenever(megaApiGateway.myUser).thenReturn(currentUser)
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(megaApiGateway.getUserAvatar(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    error,
                )
            }
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
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(megaApiGateway.getUserAvatar(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    error,
                )
            }
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
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(megaApiGateway.getUserAvatar(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    error,
                )
            }
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
            whenever(cacheGateway.buildAvatarFile(oldEmail + FileConstant.JPG_EXTENSION))
                .thenReturn(oldFile)
            whenever(cacheGateway.buildAvatarFile(newEmail + FileConstant.JPG_EXTENSION))
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
            whenever(cacheGateway.buildAvatarFile(oldEmail + FileConstant.JPG_EXTENSION))
                .thenReturn(oldFile)
            whenever(cacheGateway.buildAvatarFile(newEmail + FileConstant.JPG_EXTENSION))
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
        whenever(megaApiGateway.accountEmail).thenReturn(email)
        underTest.getMyAvatarFile(isForceRefresh = false)
        verify(megaApiGateway, times(0)).myUser
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }
        whenever(megaApiGateway.getUserAvatar(any(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                error,
            )
        }
        verify(
            cacheGateway,
            times(1)
        ).buildAvatarFile(email + FileConstant.JPG_EXTENSION)
    }

    @Test
    fun `test that getMyAvatarFile from the sdk when pass isForceRefresh as true`() = runTest {
        val expectedFile = mock<File> {
            on { absolutePath }.thenReturn("path")
            on { exists() }.thenReturn(true)
        }
        whenever(megaApiGateway.myUser).thenReturn(currentUser)
        whenever(megaApiGateway.accountEmail).thenReturn(CURRENT_USER_EMAIL)
        whenever(cacheGateway.buildAvatarFile(CURRENT_USER_EMAIL + FileConstant.JPG_EXTENSION)).thenReturn(
            expectedFile
        )
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }
        whenever(megaApiGateway.getUserAvatar(any(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                error,
            )
        }
        Truth.assertThat(underTest.getMyAvatarFile(true)).isEqualTo(expectedFile)
    }

    @Test
    fun `test that monitorUserAvatarUpdates emits correctly`() = runTest {
        val userUpdate1 = mock<MegaUser> {
            on { handle } doReturn 1L
            on { isOwnChange } doReturn 1
            on { hasChanged(MegaUser.CHANGE_TYPE_ALIAS.toLong()) } doReturn true
            on { hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong()) } doReturn true
        }
        val userUpdate2 = mock<MegaUser> {
            on { handle } doReturn 2L
            on { isOwnChange } doReturn 0
            on { hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong()) } doReturn true
        }
        val userUpdate3 = mock<MegaUser> {
            on { handle } doReturn 3L
            on { isOwnChange } doReturn 0
            on { hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME.toLong()) } doReturn true
            on { hasChanged(MegaUser.CHANGE_TYPE_ALIAS.toLong()) } doReturn true
        }
        whenever(megaApiGateway.globalUpdates).thenReturn(
            flowOf(
                GlobalUpdate.OnUsersUpdate(
                    users = arrayListOf(userUpdate1, userUpdate2, userUpdate3)
                ),
                GlobalUpdate.OnAccountUpdate,
            )
        )

        underTest.monitorUserAvatarUpdates().test {
            assertThat(awaitItem()).isEqualTo(2L)
            awaitComplete()
        }
    }
}
