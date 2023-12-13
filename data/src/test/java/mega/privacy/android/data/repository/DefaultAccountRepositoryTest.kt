package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.AccountPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.EphemeralCredentialsGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.AchievementsOverviewMapper
import mega.privacy.android.data.mapper.MegaAchievementMapper
import mega.privacy.android.data.mapper.SubscriptionOptionListMapper
import mega.privacy.android.data.mapper.UserAccountMapper
import mega.privacy.android.data.mapper.account.AccountBlockedDetailMapper
import mega.privacy.android.data.mapper.account.RecoveryKeyToFileMapper
import mega.privacy.android.data.mapper.changepassword.PasswordStrengthMapper
import mega.privacy.android.data.mapper.contact.MyAccountCredentialsMapper
import mega.privacy.android.data.mapper.login.AccountSessionMapper
import mega.privacy.android.data.mapper.login.UserCredentialsMapper
import mega.privacy.android.data.mapper.settings.CookieSettingsIntMapper
import mega.privacy.android.data.mapper.settings.CookieSettingsMapper
import mega.privacy.android.data.mapper.toAccountType
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.repository.account.DefaultAccountRepository
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.ChangeEmailException
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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalContracts
class DefaultAccountRepositoryTest {
    private lateinit var underTest: AccountRepository

    private val accountInfoWrapper =
        mock<AccountInfoWrapper> { on { accountTypeString }.thenReturn("") }
    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val localStorageGateway = mock<MegaLocalStorageGateway>()
    private val userAccountMapper = UserAccountMapper()
    private val accountTypeMapper = mock<AccountTypeMapper>()
    private val currencyMapper = ::Currency
    private val subscriptionOptionListMapper = mock<SubscriptionOptionListMapper>()
    private val megaAchievementMapper = mock<MegaAchievementMapper>()
    private val achievementsOverviewMapper = mock<AchievementsOverviewMapper>()
    private val dbHandler = mock<DatabaseHandler>()
    private val userCredentialsMapper = mock<UserCredentialsMapper>()
    private val accountSessionMapper = mock<AccountSessionMapper>()
    private val chatPreferencesGateway = mock<ChatPreferencesGateway>()
    private val callsPreferencesGateway = mock<CallsPreferencesGateway>()
    private val accountPreferencesGateway = mock<AccountPreferencesGateway>()
    private val cacheGateway = mock<CacheGateway>()
    private val passwordStrengthMapper = mock<PasswordStrengthMapper>()
    private val myAccountCredentialsMapper = mock<MyAccountCredentialsMapper>()
    private val ephemeralCredentialsGateway = mock<EphemeralCredentialsGateway>()
    private val accountBlockedDetailMapper = AccountBlockedDetailMapper()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val fileGateway = mock<FileGateway>()
    private val recoveryKeyToFileMapper = mock<RecoveryKeyToFileMapper>()
    private val cameraUploadsSettingsPreferenceGateway =
        mock<CameraUploadsSettingsPreferenceGateway>()
    private val cookieSettingsMapper = mock<CookieSettingsMapper>()
    private val cookieSettingsIntMapper = mock<CookieSettingsIntMapper>()

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

    private val appEventGateway: AppEventGateway = mock()

    private val subscriptionOption = SubscriptionOption(
        accountType = toAccountType(1),
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(13),
        currency = currencyMapper("EUR"),
    )
    private val mockEmail = "my@email.com"

    @BeforeEach
    fun resetMocks() {
        reset(
            accountInfoWrapper,
            megaApiGateway,
            megaChatApiGateway,
            megaApiFolderGateway,
            localStorageGateway,
            accountTypeMapper,
            subscriptionOptionListMapper,
            megaAchievementMapper,
            achievementsOverviewMapper,
            dbHandler,
            myAccountCredentialsMapper,
            userCredentialsMapper,
            accountSessionMapper,
            chatPreferencesGateway,
            callsPreferencesGateway,
            accountPreferencesGateway,
            cacheGateway,
            passwordStrengthMapper,
            appEventGateway,
            ephemeralCredentialsGateway,
            megaLocalRoomGateway,
            fileGateway,
            recoveryKeyToFileMapper,
            cameraUploadsSettingsPreferenceGateway,
            cookieSettingsMapper,
            cookieSettingsIntMapper,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = DefaultAccountRepository(
            context = mock(),
            myAccountInfoFacade = accountInfoWrapper,
            megaApiGateway = megaApiGateway,
            megaChatApiGateway = megaChatApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            userUpdateMapper = { UserUpdate(emptyMap()) },
            localStorageGateway = localStorageGateway,
            userAccountMapper = userAccountMapper,
            accountTypeMapper = accountTypeMapper,
            currencyMapper = currencyMapper,
            subscriptionOptionListMapper = subscriptionOptionListMapper,
            megaAchievementMapper = megaAchievementMapper,
            achievementsOverviewMapper = achievementsOverviewMapper,
            dbHandler = dbHandler,
            myAccountCredentialsMapper = myAccountCredentialsMapper,
            accountDetailMapper = mock(),
            userCredentialsMapper = userCredentialsMapper,
            accountSessionMapper = accountSessionMapper,
            chatPreferencesGateway = chatPreferencesGateway,
            callsPreferencesGateway = callsPreferencesGateway,
            accountPreferencesGateway = accountPreferencesGateway,
            cacheGateway = cacheGateway,
            passwordStrengthMapper = passwordStrengthMapper,
            appEventGateway = appEventGateway,
            ephemeralCredentialsGateway = ephemeralCredentialsGateway,
            accountBlockedDetailMapper = accountBlockedDetailMapper,
            megaLocalRoomGateway = megaLocalRoomGateway,
            fileGateway = fileGateway,
            recoveryKeyToFileMapper = recoveryKeyToFileMapper,
            cameraUploadsSettingsPreferenceGateway = cameraUploadsSettingsPreferenceGateway,
            cookieSettingsMapper = cookieSettingsMapper,
            cookieSettingsIntMapper = cookieSettingsIntMapper,
        )

    }

    @Test
    fun `test that get account does not throw exception if email is null`() = runTest {
        val expectedUserIdObj = null
        val expectedAccountTypeString = "Free"

        whenever(accountInfoWrapper.accountTypeId).thenReturn(-1)
        whenever(megaChatApiGateway.getMyEmail()).thenReturn(null)
        megaApiGateway.stub {
            onBlocking { isMasterBusinessAccount() }.thenReturn(false)
            onBlocking { getLoggedInUser() }.thenReturn(expectedUserIdObj)
        }
        whenever(accountInfoWrapper.accountTypeString).thenReturn(expectedAccountTypeString)

        assertThat(underTest.getUserAccount()).isNotNull()
    }

    @Test
    fun `test that user id is included in account info if user is logged in`() = runTest {
        val expectedUserId = 4L
        val expectedUserIdObj = UserId(expectedUserId)
        val expectedAccountTypeString = "Free"

        val user = mock<MegaUser> {
            on { handle }.thenReturn(expectedUserId)
            on { email }.thenReturn(mockEmail)
        }
        megaApiGateway.stub {
            onBlocking { isMasterBusinessAccount() }.thenReturn(false)
            onBlocking { getLoggedInUser() }.thenReturn(user)
        }
        whenever(accountInfoWrapper.accountTypeString).thenReturn(expectedAccountTypeString)

        assertThat(underTest.getUserAccount().userId).isEqualTo(expectedUserIdObj)
    }

    @Test
    fun `test that user update is returned when onUsersUpdate is called with non null user list value`() =
        runTest {
            val userList = arrayListOf(mock<MegaUser>())
            whenever(megaApiGateway.globalUpdates).thenReturn(
                flowOf(
                    GlobalUpdate.OnUsersUpdate(
                        userList
                    )
                )
            )
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

            whenever(
                subscriptionOptionListMapper(
                    request,
                    currencyMapper,
                )
            ).thenReturn(
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

    @Test
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

            whenever(
                subscriptionOptionListMapper(
                    request,
                    currencyMapper,
                )
            ).thenReturn(
                listOf(subscriptionOption)
            )

            whenever(megaApiGateway.getPricing(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            assertThrows<MegaException> {
                underTest.getSubscriptionOptions()
            }
        }

    @Test
    fun `test that account achievement data is successfully returned`() = runTest {
        val megaAchievementsDetails = mock<MegaAchievementsDetails>()
        val megaAchievement = mock<MegaAchievement>()

        whenever(
            megaAchievementMapper(
                megaAchievementsDetails,
                AchievementType.INVALID_ACHIEVEMENT,
                0L
            )
        ).thenReturn(megaAchievement)

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
                megaError
            ))
        }

        val actual = underTest.getAccountAchievements(AchievementType.INVALID_ACHIEVEMENT, 0L)
        assertThat(actual).isSameInstanceAs(megaAchievement)
    }

    @Test
    fun `test that account achievement overview is successfully returned`() = runTest {
        val megaAchievementsDetails = mock<MegaAchievementsDetails>()
        val achievementsOverview = AchievementsOverview(listOf(), listOf(), 0L, 0L, 0)


        whenever(achievementsOverviewMapper(megaAchievementsDetails))
            .thenReturn(achievementsOverview)

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
                megaError
            ))
        }

        val actual = underTest.getAccountAchievementsOverview()
        assertThat(actual).isSameInstanceAs(achievementsOverview)
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

            underTest.retryChatPendingConnections(false)
        }

    @Test
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
            assertThrows<ChatNotInitializedException> {
                underTest.retryChatPendingConnections(false)
            }
        }

    @Test
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

            assertThrows<MegaException> {
                underTest.retryChatPendingConnections(false)
            }
        }

    @Test
    fun `test that getSpecificAccountDetail returns success when MegaApi returns ERROR_OK`() =
        runTest {

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
                on { megaAccountDetails }.thenReturn(mock())
            }

            whenever(
                megaApiGateway.getSpecificAccountDetails(
                    storage = any(),
                    transfer = any(),
                    pro = any(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }


            underTest.getSpecificAccountDetail(storage = true, transfer = false, pro = false)
            verify(accountInfoWrapper).handleAccountDetail(megaRequest)
        }

    @Test
    fun `test that getSpecificAccountDetail finishes with general MegaException when MegaChatApi returns errors other than ERROR_ACCESS or ERROR_OK`() =
        runTest {

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
            }

            whenever(
                megaApiGateway.getSpecificAccountDetails(
                    storage = any(),
                    transfer = any(),
                    pro = any(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            assertThrows<MegaException> {
                underTest.getSpecificAccountDetail(storage = true, transfer = false, pro = false)
            }
        }

    @Test
    fun `test that getMyCredentials returns valid credentials if api returns valid credentials`() =
        runTest {
            val validCredentials = "KJ9hFK67vhj3cNCIUHAi8ccwciojiot4hVE5yab3"
            val expectedCredentials = myAccountCredentialsMapper(validCredentials)

            whenever(megaApiGateway.myCredentials).thenReturn(validCredentials)
            assertThat(underTest.getMyCredentials()).isEqualTo(expectedCredentials)
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
                on { megaAccountDetails }.thenReturn(mock())
            }

            whenever(
                megaApiGateway.getExtendedAccountDetails(
                    sessions = any(),
                    purchases = any(),
                    transactions = any(),
                    listener = any()
                )
            ).thenAnswer {
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

    @Test
    fun `test that getExtendedAccountDetails finishes with general MegaException when MegaChatApi returns errors other than ERROR_ACCESS or ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
            }

            whenever(
                megaApiGateway.getExtendedAccountDetails(
                    sessions = any(),
                    purchases = any(),
                    transactions = any(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            assertThrows<MegaException> {
                underTest.getExtendedAccountDetails(
                    sessions = true,
                    purchases = false,
                    transactions = false
                )
            }
        }


    @Test
    fun `test that requestAccount returns success when MegaApi returns ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_ACCOUNT_DETAILS)
                on { megaAccountDetails }.thenReturn(mock())
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

    @Test
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

            assertThrows<MegaException> {
                underTest.requestAccount()
            }
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

    @Test
    fun `test that deleteContactLink throws MegaException when error happens`() = runTest {
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EACCESS)
        }

        val megaRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_CONTACT_LINK_DELETE)
        }
        val handle = 100L

        whenever(
            megaApiGateway.contactLinkDelete(
                handle = eq(handle),
                listener = any()
            )
        ).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError,
            )
        }

        assertThrows<MegaException> {
            underTest.deleteContactLink(handle)
        }
    }

    @Test
    fun `test that deleteContactLink is success when MegaApi returns API_OK`() = runTest {
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        val megaRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_CONTACT_LINK_DELETE)
        }
        val handle = 100L

        whenever(
            megaApiGateway.contactLinkDelete(
                handle = eq(handle),
                listener = any()
            )
        ).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError,
            )
        }

        underTest.deleteContactLink(handle)
    }

    @Test
    fun `test that createContactLink returns success`() = runTest {
        val expectedHandle = 100L
        val base64Value = "mycontactlink"
        val renew = true
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }
        val megaRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_CONTACT_LINK_CREATE)
            on { nodeHandle }.thenReturn(expectedHandle)
        }
        whenever(
            megaApiGateway.contactLinkCreate(
                renew = eq(renew),
                listener = any()
            )
        ).thenAnswer {
            ((it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError
            ))
        }

        whenever(megaApiGateway.handleToBase64(expectedHandle)).thenReturn(base64Value)

        assertThat(underTest.createContactLink(renew)).isEqualTo("https://mega.nz/C!$base64Value")
    }

    @Test
    fun `test that createContactLink throws exception when MegaApi returns failure`() = runTest {
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EACCESS)
        }
        val megaRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_CONTACT_LINK_CREATE)
        }
        whenever(
            megaApiGateway.contactLinkCreate(
                renew = any(),
                listener = any()
            )
        ).thenAnswer {
            ((it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError
            ))
        }

        assertThrows<MegaException> {
            underTest.createContactLink(renew = true)
        }
    }


    @Test
    fun `test that MegaApiGateway method is invoked when getting accountEmail`() = runTest {
        underTest.getAccountEmail()
        verify(megaApiGateway).accountEmail
    }

    @Test
    fun `test that MegaApiGateway method is invoked when calling isUserLoggedIn`() = runTest {
        underTest.isUserLoggedIn()
        verify(megaApiGateway).isUserLoggedIn()
    }

    @Test
    fun `test that MegaApiGateway is invoked for getting current user and session while saving credentials`() =
        runTest {
            underTest.saveAccountCredentials()
            verify(megaApiGateway).myUser
            verify(megaApiGateway).dumpSession
        }

    @Test
    fun `test that MegaLocalStorageGateway is invoked for saving credentials and clearing ephemeral while saving credentials`() =
        runTest {
            val credentials =
                userCredentialsMapper("test@mega.nz", "AFasdffW456sdfg", null, null, "1536456")

            underTest.saveAccountCredentials()
            verify(localStorageGateway).saveCredentials(credentials)
            verify(ephemeralCredentialsGateway).clear()
        }

    @Test
    fun `test that account session is returned while saving credentials`() = runTest {
        val email = "test@mega.nz"
        val session = "AFasdffW456sdfg"
        val handle = 1536456L

        assertThat(underTest.saveAccountCredentials())
            .isEqualTo(accountSessionMapper(email, session, handle))
    }

    @Test
    fun `test that MegaLocalStorageGateway is invoked for getting account credentials`() =
        runTest {
            underTest.getAccountCredentials()
            verify(localStorageGateway).getUserCredentials()
        }

    @Test
    fun `test that changeEmail return new email when API returns success`() = runTest {
        val expectedEmail = "myEmail"
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }
        val megaRequest = mock<MegaRequest> {
            on { email }.thenReturn(expectedEmail)
        }
        whenever(megaApiGateway.changeEmail(any(), any())).thenAnswer {
            ((it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError
            ))
        }
        val actualEmail = underTest.changeEmail(expectedEmail)
        assertEquals(expectedEmail, actualEmail)
    }

    @Test
    fun `test that changeEmail throw EmailInUse exception when API returns API_EACCESS error code`() =
        runTest {
            val expectedEmail = "myEmail"
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EACCESS)
            }
            val megaRequest = mock<MegaRequest> {
                on { email }.thenReturn(expectedEmail)
            }
            whenever(megaApiGateway.changeEmail(any(), any())).thenAnswer {
                ((it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError
                ))
            }
            assertThrows<ChangeEmailException.EmailInUse> {
                underTest.changeEmail(expectedEmail)
            }
        }


    @Test
    fun `test that changeEmail throw AlreadyRequested exception when API returns API_EEXIST error code`() =
        runTest {
            val expectedEmail = "myEmail"
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EEXIST)
            }
            val megaRequest = mock<MegaRequest> {
                on { email }.thenReturn(expectedEmail)
            }
            whenever(megaApiGateway.changeEmail(any(), any())).thenAnswer {
                ((it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError
                ))
            }
            assertThrows<ChangeEmailException.AlreadyRequested> {
                underTest.changeEmail(expectedEmail)
            }
        }

    @Test
    fun `test that changeEmail throw Unknown exception when API returns common error code`() =
        runTest {
            val expectedEmail = "myEmail"
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EAGAIN)
            }
            val megaRequest = mock<MegaRequest> {
                on { email }.thenReturn(expectedEmail)
            }
            whenever(megaApiGateway.changeEmail(any(), any())).thenAnswer {
                ((it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError
                ))
            }
            assertThrows<ChangeEmailException.Unknown> {
                underTest.changeEmail(expectedEmail)
            }
        }

    @Test
    fun `test that MegaApiFolderGateway is invoked when resetting accountAuth`() = runTest {
        underTest.resetAccountAuth()
        verify(megaApiFolderGateway).setAccountAuth(null)
    }

    @Test
    fun `test that MegaLocalStorageGateway is invoked for clearing account preferences`() =
        runTest {
            underTest.clearAccountPreferences()
            verify(localStorageGateway).clearCredentials()
            verify(localStorageGateway).clearPreferences()
            verify(localStorageGateway).setFirstTime(false)
            verify(megaLocalRoomGateway).clearOffline()
            verify(localStorageGateway).clearContacts()
            verify(localStorageGateway).clearNonContacts()
            verify(localStorageGateway).clearChatItems()
            verify(localStorageGateway).clearAttributes()
            verify(megaLocalRoomGateway).deleteAllSyncRecordsTypeAny()
            verify(megaLocalRoomGateway).deleteAllCompletedTransfers()
            verify(localStorageGateway).clearChatSettings()
            verify(megaLocalRoomGateway).deleteAllBackups()
            verify(cameraUploadsSettingsPreferenceGateway).clearPreferences()
        }

    @Test
    fun `test that CallsPreferencesGateway is invoked for clearing account preferences`() =
        runTest {
            underTest.clearAccountPreferences()
            verify(callsPreferencesGateway).clearPreferences()
        }

    @Test
    fun `test that ChatPreferencesGateway is invoked for clearing account preferences`() =
        runTest {
            underTest.clearAccountPreferences()
            verify(chatPreferencesGateway).clearPreferences()
        }

    @Test
    fun `test that AccountPreferencesGateway is invoked for clearing account preferences`() =
        runTest {
            underTest.clearAccountPreferences()
            verify(accountPreferencesGateway).clearPreferences()
        }

    @Test
    fun `test that monitorShow2FADialog is invoked when get2FADialogPreference called`() = runTest {
        whenever(accountPreferencesGateway.monitorShow2FADialog()).thenReturn(flowOf(false))
        underTest.get2FADialogPreference()
        verify(accountPreferencesGateway).monitorShow2FADialog()
    }

    @Test
    fun `test that setDisplay2FADialog is invoked when update2FADialogPreference called`() =
        runTest {
            underTest.update2FADialogPreference(true)
            verify(accountPreferencesGateway).setDisplay2FADialog(true)
        }

    @Test
    fun `test that multiFactorAuthEnabled is invoked when is2FAEnabled called`() =
        runTest {
            whenever(megaApiGateway.accountEmail).thenReturn("email")
            whenever(megaApiGateway.multiFactorAuthEnabled(eq("email"), any())).thenAnswer {
                (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock {
                        on { type }.thenReturn(MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK)
                        on { flag }.thenReturn(true)
                    },
                    mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                )
            }
            val isEnabled = underTest.is2FAEnabled()
            assertThat(isEnabled).isTrue()
            verify(megaApiGateway).multiFactorAuthEnabled(eq("email"), any())
        }

    @Test
    fun `test that CacheFolderGateway is invoked when clearing app data and cache`() =
        runTest {
            underTest.clearAppDataAndCache()
            verify(cacheGateway).clearCacheDirectory()
            verify(cacheGateway).clearAppData()
            verify(cacheGateway).clearSdkCache()
        }

    @Test
    fun `test that AccountInfoWrapper is invoked when resetting account info`() =
        runTest {
            underTest.resetAccountInfo()
            verify(accountInfoWrapper).resetAccountInfo()
        }

    @Test
    fun `test that getLatestTargetPathCopyPreference is not invoked whenever latestTargetTimeStamp is over 60 minutes old`() =
        runTest {
            val latestTargetTimestamp = System.currentTimeMillis().minus(3700000)
            whenever(accountPreferencesGateway.getLatestTargetTimestampCopyPreference()).thenReturn(
                flowOf(latestTargetTimestamp)
            )
            underTest.getLatestTargetPathCopyPreference()
            verify(accountPreferencesGateway, never()).getLatestTargetPathCopyPreference()
        }

    @Test
    fun `test that getLatestTargetPathCopyPreference is invoked whenever latestTargetTimeStamp is less than 60 minutes old`() =
        runTest {
            val latestTargetTimestamp = System.currentTimeMillis().minus(3500000)
            whenever(accountPreferencesGateway.getLatestTargetTimestampCopyPreference()).thenReturn(
                flowOf(latestTargetTimestamp)
            )
            whenever(accountPreferencesGateway.getLatestTargetPathCopyPreference()).thenReturn(
                flowOf(1234)
            )
            underTest.getLatestTargetPathCopyPreference()
            verify(accountPreferencesGateway).getLatestTargetPathCopyPreference()
        }

    @Test
    fun `test that setLatestTargetPathCopyPreference is invoked when setLatestTargetPathPreference called`() =
        runTest {
            val handle = 1234L
            underTest.setLatestTargetPathCopyPreference(handle)
            verify(accountPreferencesGateway).setLatestTargetPathCopyPreference(handle)
        }

    @Test
    fun `test that ephemeralCredentialsGateway saveEphemeral invoke when calling saveEphemeral`() =
        runTest {
            val ephemeral = mock<EphemeralCredentials>()
            underTest.saveEphemeral(ephemeral)
            verify(ephemeralCredentialsGateway).save(ephemeral)
        }

    @Test
    fun `test that ephemeralCredentialsGateway clear invoke when calling clearEphemeral`() =
        runTest {
            underTest.clearEphemeral()
            verify(ephemeralCredentialsGateway).clear()
        }

    @Test
    fun `test that ephemeralCredentialsGateway monitorEphemeralCredentials invoke when calling monitorEphemeralCredentials`() =
        runTest {
            underTest.monitorEphemeralCredentials()
            verify(ephemeralCredentialsGateway).monitorEphemeralCredentials()
        }

    @Test
    fun `test that appEventGateway monitorRefreshSession invoke when calling monitorRefreshSession`() =
        runTest {
            underTest.monitorRefreshSession()
            verify(appEventGateway).monitorRefreshSession()
        }

    @Test
    fun `test that appEventGateway broadcastRefreshSession invoke when calling broadcastRefreshSession`() =
        runTest {
            underTest.broadcastRefreshSession()
            verify(appEventGateway).broadcastRefreshSession()
        }

    @Test
    fun `test that invoke correctly when call renameMkFileIfNeeded`() =
        runTest {
            val relativePath = "/Mega.txt"
            val newName = "newName"
            val file = mock<File>()
            whenever(fileGateway.buildExternalStorageFile(relativePath)).thenReturn(file)
            underTest.renameRecoveryKeyFile(relativePath, newName)
            verify(fileGateway).buildExternalStorageFile(relativePath)
            verify(fileGateway).renameFile(file, newName)
        }

    @Test
    fun `test that valid file is returned when getRecoveryKeyFile is invoked and recovery key is valid `() =
        runTest {
            val recoveryKey = "qwerty"
            val file = mock<File>()
            whenever(megaApiGateway.getExportMasterKey()).thenReturn(recoveryKey)
            whenever(recoveryKeyToFileMapper(recoveryKey)).thenReturn(file)
            val result = underTest.getRecoveryKeyFile()
            assertThat(result).isEqualTo(file)
        }

    @Test
    fun `test that null is returned when getRecoveryKeyFile is invoked and getExportMasterKey is null`() =
        runTest {
            whenever(megaApiGateway.getExportMasterKey()).thenReturn(null)
            val result = underTest.getRecoveryKeyFile()
            assertThat(result).isNull()
        }

    @Test
    fun `test that isCookieBannerEnabled returns general MegaException when MegaApi returns errors other than API_EACCESS or API_OK`() =
        runTest {

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EEXIST)
            }

            whenever(
                megaApiGateway.getMiscFlags(listener = any())
            ).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaError,
                )
            }

            assertThrows<MegaException> {
                underTest.isCookieBannerEnabled()
            }
        }


    fun provideGetCookieSettingsParameters() = listOf(
        Arguments.of(
            MegaError.API_OK, mutableSetOf(CookieType.ADVERTISEMENT, CookieType.ANALYTICS)
        ),
        Arguments.of(MegaError.API_ENOENT, emptySet<CookieType>()),
    )

    @ParameterizedTest(name = "return cookie settings: {1} when MegaApi returns error code: {0}")
    @MethodSource("provideGetCookieSettingsParameters")
    fun `test that getCookieSettings returns the right cookie settings when MegaApi returns specific Mega Errors`(
        input: Int,
        expectedCookieSettings: MutableSet<CookieType>,
    ) = runTest {

        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(input)
        }

        whenever(cookieSettingsMapper(any())).thenReturn(expectedCookieSettings)
        whenever(
            megaApiGateway.getCookieSettings(listener = any())
        ).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaError,
            )
        }

        val result = underTest.getCookieSettings()
        assertThat(result).isEqualTo(expectedCookieSettings)
    }

    @Test
    fun `test that getCookieSettings returns general MegaException when MegaApi returns errors other than API_ENOENT or API_OK`() =
        runTest {

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EEXIST)
            }

            whenever(
                megaApiGateway.getCookieSettings(listener = any())
            ).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaError,
                )
            }

            assertThrows<MegaException> {
                underTest.getCookieSettings()
            }
        }

    private fun provideGetMiscFlagsMegaErrors() = listOf(
        Arguments.of(MegaError.API_OK),
        Arguments.of(MegaError.API_EACCESS),
    )

    @ParameterizedTest(name = "MegaApi returns error code: {0}")
    @MethodSource("provideGetMiscFlagsMegaErrors")
    fun `test that isCookieBannerEnabled returns success when MegaApi returns specific Mega Errors`(
        input: Int,
    ) = runTest {
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(input)
        }

        whenever(
            megaApiGateway.getMiscFlags(listener = any())
        ).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaError,
            )
        }

        underTest.isCookieBannerEnabled()
        verify(megaApiGateway).isCookieBannerEnabled()
    }

    @Test
    fun `test that setCookieSettings is success when MegaApi returns API_OK`() = runTest {
        val cookieSettings = mutableSetOf(CookieType.ADVERTISEMENT, CookieType.ANALYTICS)
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(
            megaApiGateway.setCookieSettings(
                any(),
                listener = any()
            )
        ).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaError,
            )
        }

        underTest.setCookieSettings(cookieSettings)
    }

    @Test
    fun `test that setCookieSettings returns general MegaException when MegaApi returns errors other than API_OK`() =
        runTest {

            val cookieSettings = mutableSetOf(CookieType.ADVERTISEMENT, CookieType.ANALYTICS)
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EEXIST)
            }

            whenever(
                megaApiGateway.setCookieSettings(
                    any(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaError,
                )
            }

            assertThrows<MegaException> {
                underTest.setCookieSettings(cookieSettings)
            }
        }


}
