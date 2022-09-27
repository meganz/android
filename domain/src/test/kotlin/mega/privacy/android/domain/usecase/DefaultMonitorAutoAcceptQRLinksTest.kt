package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorAutoAcceptQRLinksTest {
    private lateinit var underTest: MonitorAutoAcceptQRLinks
    private val fetchAutoAcceptQRLinks = mock<FetchAutoAcceptQRLinks>()
    private val accountRepository = mock<AccountRepository> {
        on { monitorUserUpdates() }.thenReturn(
            emptyFlow()
        )
    }

    private val userAccount = UserAccount(
        userId = UserId(1),
        email = "email@email.com",
        isBusinessAccount = false,
        isMasterBusinessAccount = false,
        accountTypeIdentifier = AccountType.PRO_I,
        accountTypeString = "Free"
    )

    @Before
    fun setUp() {
        underTest = DefaultMonitorAutoAcceptQRLinks(
            fetchAutoAcceptQRLinks = fetchAutoAcceptQRLinks,
            getAccountDetails = mock { onBlocking { invoke(false) }.thenReturn(userAccount) },
            accountRepository = accountRepository
        )
    }

    @Test
    fun `test that value from fetch use case is returned`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)

        underTest().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that value is fetched if user qr setting update event is received`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true, false)

        whenever(accountRepository.monitorUserUpdates()).thenReturn(
            flowOf(
                UserUpdate(
                    mapOf(userAccount.userId!! to listOf(UserChanges.ContactLinkVerification))
                )
            )
        )

        underTest().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that only contacts links updates cause updates to be emitted`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)

        whenever(accountRepository.monitorUserUpdates()).thenReturn(
            flowOf(
                UserUpdate(
                    mapOf(userAccount.userId!! to listOf(UserChanges.Avatar))
                )
            )
        )

        underTest().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that only updates to the current user causes updates to be emitted`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true, false)

        whenever(accountRepository.monitorUserUpdates()).thenReturn(
            flowOf(
                UserUpdate(
                    mapOf(
                        userAccount.userId!! to listOf(UserChanges.ContactLinkVerification),
                        UserId(userAccount.userId!!.id + 1) to listOf(UserChanges.ContactLinkVerification),
                    )
                )
            )
        )

        underTest().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
        }
    }


}