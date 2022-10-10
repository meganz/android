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
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.domain.entity.SubscriptionPlan
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.CurrencyMapper
import mega.privacy.android.data.mapper.SkuMapper
import mega.privacy.android.data.mapper.SubscriptionPlanListMapper
import mega.privacy.android.data.mapper.SubscriptionPlanMapper
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaPricing
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultAccountRepositoryTest {
    private lateinit var underTest: AccountRepository

    private val accountInfoWrapper =
        mock<AccountInfoWrapper> { on { accountTypeString }.thenReturn("") }
    private val megaApiGateway = mock<MegaApiGateway>()
    private val userAccountMapper = ::UserAccount
    private val accountTypeMapper = mock<AccountTypeMapper>()
    private val subscriptionPlanMapper = mock<SubscriptionPlanMapper>()
    private val currencyMapper = mock<CurrencyMapper>()
    private val skuMapper = mock<SkuMapper>()
    private val subscriptionPlanListMapper = mock<SubscriptionPlanListMapper>()

    private val pricing = mock<MegaPricing> {
        on { numProducts }.thenReturn(1)
        on { getHandle(0) }.thenReturn(1560943707714440503)
        on { getProLevel(0) }.thenReturn(1)
        on { getMonths(0) }.thenReturn(1)
        on { getGBStorage(0) }.thenReturn(450)
        on { getGBTransfer(0) }.thenReturn(450)
        on { getAmount(0) }.thenReturn(13)
    }

    private val currency = mock<MegaCurrency> {
        on { currencyName }.thenReturn("EUR")
    }

    private val subscriptionPlan = SubscriptionPlan(
        handle = 1560943707714440503,
        level = accountTypeMapper(1),
        months = 1,
        storage = 450,
        transfer = 450,
        amount = 13,
        currency = currencyMapper("EUR"),
        sku = skuMapper(accountTypeMapper(1), 1)
    )

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
            userAccountMapper = userAccountMapper,
            accountTypeMapper = accountTypeMapper,
            subscriptionPlanMapper = subscriptionPlanMapper,
            currencyMapper = currencyMapper,
            skuMapper = skuMapper,
            subscriptionPlanListMapper = subscriptionPlanListMapper,
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

    @Test
    fun `test that is business account active returns true if api returns true`() =
        runTest {
            whenever(megaApiGateway.isBusinessAccountActive()).thenReturn(true)
            assertThat(underTest.isBusinessAccountActive()).isTrue()
        }

    @Test
    fun `test that is business account active returns false if api returns false`() = runTest {
        whenever(megaApiGateway.isBusinessAccountActive()).thenReturn(false)
        assertThat(underTest.isBusinessAccountActive()).isFalse()
    }

    @Test
    fun `test that get subscription plans returns successfully if no error is thrown`() =
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { pricing }.thenReturn(pricing)
                on { currency }.thenReturn(currency)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(subscriptionPlanListMapper(
                request,
                subscriptionPlanMapper,
                currencyMapper,
                skuMapper,
            )).thenReturn(
                listOf(subscriptionPlan)
            )

            whenever(megaApiGateway.getPricing(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            val actual = underTest.getSubscriptionPlans()

            assertThat(actual[0]).isSameInstanceAs(subscriptionPlan)
        }

    @Test(expected = MegaException::class)
    fun `test that get subscription plans throws an exception when the api returns an error`() =
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { pricing }.thenReturn(pricing)
                on { currency }.thenReturn(currency)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(subscriptionPlanListMapper(
                request,
                subscriptionPlanMapper,
                currencyMapper,
                skuMapper,
            )).thenReturn(
                listOf(subscriptionPlan)
            )

            whenever(megaApiGateway.getPricing(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            underTest.getSubscriptionPlans()
        }
}