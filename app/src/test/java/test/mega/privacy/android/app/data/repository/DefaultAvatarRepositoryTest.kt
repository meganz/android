package test.mega.privacy.android.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.data.repository.DefaultAvatarRepository
import nz.mega.sdk.MegaUser
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertTrue

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

    @Before
    fun setUp() {
        whenever(megaApiGateway.accountEmail).thenReturn(CURRENT_USER_EMAIL)
        whenever(cacheFolderGateway.buildAvatarFile(any())).thenReturn(File(""))
    }

    @Test
    fun `when globalUpdates emit OnAccountUpdate then my avatar file return null`() = runTest {
        val flow = flowOf(GlobalUpdate.OnAccountUpdate)
        whenever(megaApiGateway.globalUpdates).thenReturn(flow)
        underTest = DefaultAvatarRepository(
            megaApiGateway = megaApiGateway,
            cacheFolderGateway = cacheFolderGateway,
            sharingScope = TestScope(),
            ioDispatcher = UnconfinedTestDispatcher()
        )
        assertTrue(underTest.monitorMyAvatarFile().first() == null)
    }

    @Test
    fun `when globalUpdates emit OnUsersUpdate with current user then my avatar file return not null`() =
        runTest {
            val flow = flowOf(GlobalUpdate.OnUsersUpdate(users = arrayListOf(currentUser)))
            whenever(megaApiGateway.globalUpdates).thenReturn(flow)
            whenever(megaApiGateway.getUserAvatar(any(), any())).thenReturn(true)
            underTest = DefaultAvatarRepository(
                megaApiGateway = megaApiGateway,
                cacheFolderGateway = cacheFolderGateway,
                sharingScope = TestScope(),
                ioDispatcher = UnconfinedTestDispatcher()
            )
            assertTrue(underTest.monitorMyAvatarFile().first() != null)
        }

    @Test
    fun `when globalUpdates emit OnUsersUpdate with another user then my avatar file return null`() =
        runTest {
            val anotherUser = mock<MegaUser> {
                on { it.isOwnChange }.thenReturn(0)
                on { it.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) }.thenReturn(true)
                on { it.email }.thenReturn("anotherUser")
            }
            val flow = flowOf(GlobalUpdate.OnUsersUpdate(users = arrayListOf(anotherUser)))
            whenever(megaApiGateway.globalUpdates).thenReturn(flow)
            whenever(megaApiGateway.getUserAvatar(any(), any())).thenReturn(true)
            underTest = DefaultAvatarRepository(
                megaApiGateway = megaApiGateway,
                cacheFolderGateway = cacheFolderGateway,
                sharingScope = TestScope(),
                ioDispatcher = UnconfinedTestDispatcher()
            )
            assertTrue(underTest.monitorMyAvatarFile().first() == null)
        }

    @Test
    fun `when globalUpdates emit OnUsersUpdate with current user and isOwnChange more than 0 then my avatar file return null`() =
        runTest {
            val flow = flowOf(GlobalUpdate.OnUsersUpdate(users = arrayListOf(currentUser)))
            whenever(currentUser.isOwnChange).thenReturn(1)
            whenever(megaApiGateway.globalUpdates).thenReturn(flow)
            whenever(megaApiGateway.getUserAvatar(any(), any())).thenReturn(true)
            underTest = DefaultAvatarRepository(
                megaApiGateway = megaApiGateway,
                cacheFolderGateway = cacheFolderGateway,
                sharingScope = TestScope(),
                ioDispatcher = UnconfinedTestDispatcher()
            )
            assertTrue(underTest.monitorMyAvatarFile().first() == null)
        }
}