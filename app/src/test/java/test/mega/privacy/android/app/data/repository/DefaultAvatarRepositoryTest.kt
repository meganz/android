package test.mega.privacy.android.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.data.repository.DefaultAvatarRepository
import mega.privacy.android.data.gateway.CacheFolderGateway
import nz.mega.sdk.MegaUser
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.di.TestWrapperModule
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultAvatarRepositoryTest {
    companion object {
        private const val CURRENT_USER_EMAIL = "CURRENT_USER_EMAIL"
    }

    private lateinit var underTest: DefaultAvatarRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val cacheFolderGateway = mock<CacheFolderGateway>()
    private val currentUser = mock<MegaUser> {
        on { it.isOwnChange }.thenReturn(0)
        on { it.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) }.thenReturn(true)
        on { it.email }.thenReturn(CURRENT_USER_EMAIL)
    }
    private val sharedFlow = MutableSharedFlow<GlobalUpdate>()

    @Before
    fun setUp() {
        whenever(megaApiGateway.accountEmail).thenReturn(CURRENT_USER_EMAIL)
        whenever(cacheFolderGateway.buildAvatarFile(any())).thenReturn(File(""))
        whenever(megaApiGateway.globalUpdates).thenReturn(sharedFlow)
        underTest = DefaultAvatarRepository(
            megaApiGateway = megaApiGateway,
            avatarWrapper = TestWrapperModule.avatarWrapper,
            bitmapFactoryWrapper = TestWrapperModule.bitmapFactoryWrapper,
            cacheFolderGateway = cacheFolderGateway,
            sharingScope = TestScope(),
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `when globalUpdates emit OnAccountUpdate then monitorMyAvatarFile not emit`() = runTest {
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
            val avatarFile = MutableStateFlow<File?>(null)
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest.monitorMyAvatarFile().collect { file ->
                    avatarFile.value = file
                }
            }
            sharedFlow.emit(GlobalUpdate.OnUsersUpdate(users = arrayListOf(currentUser)))
            whenever(megaApiGateway.getUserAvatar(any(), any())).thenReturn(true)
            delay(300L)
            assertTrue(avatarFile.value != null)
            collectJob.cancel()
        }

    @Test
    fun `when globalUpdates emit OnUsersUpdate with another user then monitorMyAvatarFile not emit`() =
        runTest {
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest.monitorMyAvatarFile().collect {
                    fail("monitorMyAvatarFile emit")
                }
            }
            val anotherUser = mock<MegaUser> {
                on { it.isOwnChange }.thenReturn(0)
                on { it.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) }.thenReturn(true)
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
}