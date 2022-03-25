package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.data.facade.AccountInfoWrapper
import mega.privacy.android.app.data.gateway.MonitorMultiFactorAuth
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.app.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class DefaultAccountRepositoryTest{
    private lateinit var underTest: AccountRepository

    private val accountInfoWrapper = mock<AccountInfoWrapper>()
    private val megaApiWrapper = mock<MegaApiGateway>()

    @Before
    fun setUp() {
        underTest = DefaultAccountRepository(
            myAccountInfoFacade = accountInfoWrapper,
            apiFacade = megaApiWrapper,
            context = mock(),
            monitorMultiFactorAuth = MonitorMultiFactorAuth()
        )
    }

    @Test
    fun `test that get account does not throw exception if email is null`() {
        whenever(accountInfoWrapper.accountTypeId).thenReturn(-1)
        whenever(megaApiWrapper.accountEmail).thenReturn(null)

        assertThat(underTest.getUserAccount()).isNotNull()
    }
}