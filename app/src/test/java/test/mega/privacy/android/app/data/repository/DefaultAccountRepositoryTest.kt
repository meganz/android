package test.mega.privacy.android.app.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.facade.AccountInfoWrapper
import mega.privacy.android.app.data.gateway.MonitorMultiFactorAuth
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaUser
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
            megaChatApiGateway = mock(),
            monitorMultiFactorAuth = MonitorMultiFactorAuth(),
            ioDispatcher = UnconfinedTestDispatcher(),
            userUpdateMapper = { UserUpdate(emptyMap()) },
            localStorageGateway = mock(),
        )
    }

    @Test
    fun `test that get account does not throw exception if email is null`() = runTest {
        whenever(accountInfoWrapper.accountTypeId).thenReturn(-1)
        whenever(megaApiGateway.getLoggedInUser()).thenReturn(null)
        whenever(accountInfoWrapper.accountTypeString).thenReturn("Free")

        assertThat(underTest.getUserAccount()).isNotNull()
    }

    @Test
    fun `test that user id is included in account info if user is logged in`() = runTest {
        val expectedUserId = 4L
        val user = mock<MegaUser> { on { handle }.thenReturn(expectedUserId) }
        whenever(megaApiGateway.getLoggedInUser()).thenReturn(user)

        assertThat(underTest.getUserAccount().userId).isEqualTo(UserId(expectedUserId))
    }

    @Test
    fun `test that user update is returned when onUsersUpdate is called with non null user list value`() =
        runTest {
            val userList = arrayListOf(mock<MegaUser>())
            whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(GlobalUpdate.OnUsersUpdate(
                userList)))
            underTest.monitorUserUpdates().test {
                assertThat(awaitItem()).isInstanceOf(UserUpdate::class.java)
                awaitComplete()
            }
        }

    @Test
    fun `test that no user update is returned when onUsersUpdate is returned with null user list value`() =
        runTest {
            whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(GlobalUpdate.OnUsersUpdate(null)))
            underTest.monitorUserUpdates().test {
                awaitComplete()
            }
        }

}