package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.CurrencyMapper
import mega.privacy.android.data.mapper.MegaAchievementMapper
import mega.privacy.android.data.mapper.SkuMapper
import mega.privacy.android.data.mapper.SubscriptionPlanListMapper
import mega.privacy.android.data.mapper.SubscriptionPlanMapper
import mega.privacy.android.data.mapper.toAccountType
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.Pricing
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionPlan
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.ChatNotInitializedException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
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
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val userAccountMapper = ::UserAccount
    private val accountTypeMapper = mock<AccountTypeMapper>()
    private val subscriptionPlanMapper = mock<SubscriptionPlanMapper>()
    private val currencyMapper = mock<CurrencyMapper>()
    private val skuMapper = mock<SkuMapper>()
    private val subscriptionPlanListMapper = mock<SubscriptionPlanListMapper>()
    private val megaAchievementMapper = mock<MegaAchievementMapper>()

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
        pricing = Pricing(
            amount = 13,
            currency = currencyMapper("EUR"),
            sku = skuMapper(toAccountType(1), 1)),
        subscription = Subscription(
            handle = 1560943707714440503,
            level = toAccountType(1),
            months = 1,
            storage = 450,
            transfer = 450,
        ),
    )

    @Before
    fun setUp() {
        underTest = DefaultAccountRepository(
            myAccountInfoFacade = accountInfoWrapper,
            megaApiGateway = megaApiGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            userUpdateMapper = { UserUpdate(emptyMap()) },
            localStorageGateway = mock(),
            userAccountMapper = userAccountMapper,
            accountTypeMapper = accountTypeMapper,
            subscriptionPlanMapper = subscriptionPlanMapper,
            currencyMapper = currencyMapper,
            skuMapper = skuMapper,
            subscriptionPlanListMapper = subscriptionPlanListMapper,
            megaAchievementMapper = megaAchievementMapper,
            dbHandler = mock()
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

    @Test
    fun `test that account achievement data is successfully returned`() = runTest {
        val megaAchievementsDetails = mock<MegaAchievementsDetails>()
        val megaAchievement = mock<MegaAchievement>()

        whenever(megaAchievementMapper(megaAchievementsDetails,
            AchievementType.INVALID_ACHIEVEMENT,
            0L)).thenReturn(megaAchievement)

        val megaApiJava = mock<MegaApiJava>()
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        val megaRequest = mock<MegaRequest> {
            on { getMegaAchievementsDetails() }.thenReturn(megaAchievementsDetails)
        }

        whenever(megaApiGateway.getAccountAchievements(any())).thenAnswer {
            ((it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                megaRequest,
                megaError))
        }

        val actual = underTest.getAccountAchievements(AchievementType.INVALID_ACHIEVEMENT, 0L)
        assertThat(actual).isSameInstanceAs(megaAchievement)
    }

    @Test
    fun `test that retryPendingConnections returns success when MegaChatApi returns ERROR_OK`() =
        runTest {

            val megaError = mock<MegaChatError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaChatRequest> {
                on { type }.thenReturn(MegaChatRequest.TYPE_RETRY_PENDING_CONNECTIONS)
            }

            whenever(megaChatApiGateway.retryPendingConnections(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.retryPendingConnections(false)
        }

    @Test(expected = ChatNotInitializedException::class)
    fun `test that retryPendingConnections finishes with ChatNotInitializedException when MegaChatApi returns ERROR_ACCESS`() =
        runTest {

            val megaError = mock<MegaChatError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_ACCESS)
            }

            val megaRequest = mock<MegaChatRequest> {
                on { type }.thenReturn(MegaChatRequest.TYPE_RETRY_PENDING_CONNECTIONS)
            }

            whenever(megaChatApiGateway.retryPendingConnections(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.retryPendingConnections(false)
        }

    @Test(expected = MegaException::class)
    fun `test that retryPendingConnections finishes with general MegaException when MegaChatApi returns errors other than ERROR_ACCESS or ERROR_OK`() =
        runTest {

            val megaError = mock<MegaChatError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }

            val megaRequest = mock<MegaChatRequest> {
                on { type }.thenReturn(MegaChatRequest.TYPE_RETRY_PENDING_CONNECTIONS)
            }

            whenever(megaChatApiGateway.retryPendingConnections(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.retryPendingConnections(false)
        }

}