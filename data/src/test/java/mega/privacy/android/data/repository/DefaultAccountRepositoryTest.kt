package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.MegaAchievementMapper
import mega.privacy.android.data.mapper.SubscriptionOptionListMapper
import mega.privacy.android.data.mapper.toAccountType
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyPoint
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
import org.mockito.kotlin.verify
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
    private val currencyMapper = ::Currency
    private val subscriptionOptionListMapper = mock<SubscriptionOptionListMapper>()
    private val megaAchievementMapper = mock<MegaAchievementMapper>()
    private val dbHandler = mock<DatabaseHandler>()

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

    private val subscriptionOption = SubscriptionOption(
        accountType = toAccountType(1),
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(13),
        currency = currencyMapper("EUR"),
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
            currencyMapper = currencyMapper,
            subscriptionOptionListMapper = subscriptionOptionListMapper,
            megaAchievementMapper = megaAchievementMapper,
            dbHandler = dbHandler,
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
    fun `test that get subscription options returns successfully if no error is thrown`() =
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { pricing }.thenReturn(pricing)
                on { currency }.thenReturn(currency)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(subscriptionOptionListMapper(
                request,
                currencyMapper,
            )).thenReturn(
                listOf(subscriptionOption)
            )

            whenever(megaApiGateway.getPricing(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            val actual = underTest.getSubscriptionOptions()

            assertThat(actual[0]).isSameInstanceAs(subscriptionOption)
        }

    @Test(expected = MegaException::class)
    fun `test that get subscription options throws an exception when the api returns an error`() =
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { pricing }.thenReturn(pricing)
                on { currency }.thenReturn(currency)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(subscriptionOptionListMapper(
                request,
                currencyMapper,
            )).thenReturn(
                listOf(subscriptionOption)
            )

            whenever(megaApiGateway.getPricing(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            underTest.getSubscriptionOptions()
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

    @Test
    fun `test that getSpecificAccountDetail returns success when MegaApi returns ERROR_OK`() =
        runTest {

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
            }

            whenever(megaApiGateway.getSpecificAccountDetails(
                storage = any(),
                transfer = any(),
                pro = any(),
                listener = any())).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }


            underTest.getSpecificAccountDetail(storage = true, transfer = false, pro = false)
            verify(accountInfoWrapper).handleAccountDetail(megaRequest)
        }

    @Test(expected = MegaException::class)
    fun `test that getSpecificAccountDetail finishes with general MegaException when MegaChatApi returns errors other than ERROR_ACCESS or ERROR_OK`() =
        runTest {

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
            }

            whenever(megaApiGateway.getSpecificAccountDetails(
                storage = any(),
                transfer = any(),
                pro = any(),
                listener = any())).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.getSpecificAccountDetail(storage = true, transfer = false, pro = false)
        }

    @Test
    fun `test that getMyCredentials returns valid credentials if api returns valid credentials`() =
        runTest {
            val validCredentials = "KJ9hFK67vhj3cNCIUHAi8ccwciojiot4hVE5yab3"

            whenever(megaApiGateway.myCredentials).thenReturn(validCredentials)
            assertThat(underTest.getMyCredentials()).isEqualTo(validCredentials)
        }

    @Test
    fun `test that getMyCredentials returns null if api returns null`() =
        runTest {
            whenever(megaApiGateway.myCredentials).thenReturn(null)
            assertThat(underTest.getMyCredentials()).isNull()
        }

    @Test
    fun `test that getExtendedAccountDetails returns success when MegaApi returns ERROR_OK`() =
        runTest {

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
            }

            whenever(megaApiGateway.getExtendedAccountDetails(
                sessions = any(),
                purchases = any(),
                transactions = any(),
                listener = any())).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.getExtendedAccountDetails(
                sessions = true,
                purchases = false,
                transactions = false
            )
            verify(accountInfoWrapper).handleAccountDetail(megaRequest)
        }

    @Test(expected = MegaException::class)
    fun `test that getExtendedAccountDetails finishes with general MegaException when MegaChatApi returns errors other than ERROR_ACCESS or ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
            }

            whenever(megaApiGateway.getExtendedAccountDetails(
                sessions = any(),
                purchases = any(),
                transactions = any(),
                listener = any())).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.getExtendedAccountDetails(
                sessions = true,
                purchases = false,
                transactions = false
            )
        }


    @Test
    fun `test that requestAccount returns success when MegaApi returns ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
            }

            whenever(megaApiGateway.getAccountDetails(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.requestAccount()
            verify(accountInfoWrapper).handleAccountDetail(megaRequest)
        }

    @Test(expected = MegaException::class)
    fun `test that requestAccount finishes with general MegaException when MegaChatApi returns errors other than ERROR_ACCESS or ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
            }

            whenever(megaApiGateway.getAccountDetails(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.requestAccount()
        }

    @Test
    fun `test resetAccountDetailsTimeStamp invoke correct method`() = runTest {
        underTest.resetAccountDetailsTimeStamp()
        verify(dbHandler).resetAccountDetailsTimeStamp()
    }

    @Test
    fun `test resetExtendedAccountDetailsTimestamp invoke correct method`() = runTest {
        underTest.resetExtendedAccountDetailsTimestamp()
        verify(dbHandler).resetExtendedAccountDetailsTimestamp()
    }
}