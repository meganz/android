package test.mega.privacy.android.app.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.facade.AccountInfoWrapper
import mega.privacy.android.app.data.gateway.MonitorMultiFactorAuth
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.mapper.fromMegaUserChangeFlags
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.app.domain.entity.user.UserChanges
import mega.privacy.android.app.domain.entity.user.UserId
import mega.privacy.android.app.domain.entity.user.UserUpdate
import mega.privacy.android.app.domain.repository.AccountRepository
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaUser
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultAccountRepositoryTest {
    private lateinit var underTest: AccountRepository

    private val accountInfoWrapper = mock<AccountInfoWrapper>()
    private val megaApiGateway = mock<MegaApiGateway>()

    @Before
    fun setUp() {
        underTest = DefaultAccountRepository(
            myAccountInfoFacade = accountInfoWrapper,
            megaApiGateway = megaApiGateway,
            context = mock(),
            monitorMultiFactorAuth = MonitorMultiFactorAuth(),
            ioDispatcher = UnconfinedTestDispatcher(),
            userChangesMapper = ::fromMegaUserChangeFlags
        )
    }

    @Test
    fun `test that get account does not throw exception if email is null`() = runTest {
        whenever(accountInfoWrapper.accountTypeId).thenReturn(-1)
        whenever(megaApiGateway.getLoggedInUser()).thenReturn(null)

        assertThat(underTest.getUserAccount()).isNotNull()
    }

    @Test
    fun `test that user id is included in account info if user is logged in`() = runTest{
        val expectedUserId = 4L
        val user = mock<MegaUser> { on { handle }.thenReturn(expectedUserId) }
        whenever(megaApiGateway.getLoggedInUser()).thenReturn(user)

        assertThat(underTest.getUserAccount().userId).isEqualTo(UserId(expectedUserId))
    }

    @Test
    fun `test that a listener is added on subscribe to user updates`() = runTest {
        underTest.monitorUserUpdates().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(megaApiGateway).addGlobalListener(any())
    }

    @Test
    fun `test that listener is removed on cancel of user updates`() = runTest {
        underTest.monitorUserUpdates().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(megaApiGateway).removeGlobalListener(any())
    }

    @Test
    fun `test that user update is returned when onUsersUpdate is called with non null value`() =
        runTest {
            val listenerCaptor = argumentCaptor<MegaGlobalListenerInterface>()
            val userList = arrayListOf(mock<MegaUser>())
            underTest.monitorUserUpdates().test {
                verify(megaApiGateway).addGlobalListener(listenerCaptor.capture())
                listenerCaptor.lastValue.onUsersUpdate(mock(), userList)

                val actual = awaitItem()
                assertThat(actual).isInstanceOf(UserUpdate::class.java)
            }
        }

    @Test
    fun `test that no user update is returned when onUsersUpdate is called with null value`() =
        runTest {
            val listenerCaptor = argumentCaptor<MegaGlobalListenerInterface>()
            underTest.monitorUserUpdates().test {
                verify(megaApiGateway).addGlobalListener(listenerCaptor.capture())
                listenerCaptor.lastValue.onUsersUpdate(mock(), null)
            }
        }

    @Test
    fun `test that duplicate users are combined`() = runTest {

        val listenerCaptor = argumentCaptor<MegaGlobalListenerInterface>()
        val user1 = mock<MegaUser> {
            on { handle }.thenReturn(1L)
            on { changes }.thenReturn(MegaUser.CHANGE_TYPE_ALIAS, MegaUser.CHANGE_TYPE_AVATAR)
        }
        val user2 = mock<MegaUser> {
            on { handle }.thenReturn(2L)
            on { changes }.thenReturn(MegaUser.CHANGE_TYPE_ALIAS)
        }

        val userList = arrayListOf(user1, user2, user1)
        underTest.monitorUserUpdates().test {
            verify(megaApiGateway).addGlobalListener(listenerCaptor.capture())
            listenerCaptor.lastValue.onUsersUpdate(mock(), userList)

            val actual = awaitItem()

            assertThat(actual).isInstanceOf(UserUpdate::class.java)
            assertThat(actual.changes.size).isEqualTo(2)
            assertThat(actual.changes[UserId(1L)]).containsExactly(
                UserChanges.Alias,
                UserChanges.Avatar
            )
            assertThat(actual.changes[UserId(2L)]).containsExactly(UserChanges.Alias)

        }
    }

}

