package mega.privacy.android.data.repository

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

/**
 * Test class for [DefaultSettingsRepository]
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSettingsRepositoryTest {
    private lateinit var underTest: DefaultSettingsRepository

    private val databaseHandler: DatabaseHandler = mock {
        on { preferences }.thenReturn(mock())
    }
    private val context: Context = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val megaLocalStorageGateway: MegaLocalStorageGateway = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val chatPreferencesGateway: ChatPreferencesGateway = mock()
    private val callsPreferencesGateway: CallsPreferencesGateway = mock()
    private val appPreferencesGateway: AppPreferencesGateway = mock()
    private val fileGateway: FileGateway = mock()
    private val uiPreferencesGateway: UIPreferencesGateway = mock()
    private val startScreenMapper: StartScreenMapper = mock()
    private val fileManagementPreferencesGateway: FileManagementPreferencesGateway = mock()
    private val fileVersionsOptionCache: Cache<Boolean> = mock()
    private val myAccountInfoFacade: AccountInfoWrapper = mock()

    @BeforeAll
    fun setUp() {
        underTest = DefaultSettingsRepository(
            databaseHandler = { databaseHandler },
            context = context,
            megaApiGateway = megaApiGateway,
            megaLocalStorageGateway = megaLocalStorageGateway,
            ioDispatcher = ioDispatcher,
            chatPreferencesGateway = chatPreferencesGateway,
            callsPreferencesGateway = callsPreferencesGateway,
            appPreferencesGateway = appPreferencesGateway,
            fileGateway = fileGateway,
            uiPreferencesGateway = uiPreferencesGateway,
            startScreenMapper = startScreenMapper,
            fileManagementPreferencesGateway = fileManagementPreferencesGateway,
            fileVersionsOptionCache = fileVersionsOptionCache,
            myAccountInfoFacade = myAccountInfoFacade
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            databaseHandler,
            context,
            megaApiGateway,
            megaLocalStorageGateway,
            chatPreferencesGateway,
            callsPreferencesGateway,
            appPreferencesGateway,
            fileGateway,
            uiPreferencesGateway,
            startScreenMapper,
            fileManagementPreferencesGateway,
            fileVersionsOptionCache,
        )
    }

    @Test
    fun `test that setFileVersionsOption success if no error is thrown`() = runTest {
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest>()
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.setFileVersionsOption(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        underTest.enableFileVersionsOption(true)
    }

    @Test
    fun `test that calling setFileVersionsOption thrown an exception if the api does not return successfully`() =
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(megaApiGateway.setFileVersionsOption(any(), any())).thenAnswer {
                (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            assertThrows<MegaException> {
                underTest.enableFileVersionsOption(true)
            }
        }

    @ParameterizedTest(name = "set to: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that set ask before large downloads sets the value to the storage gateway`(ask: Boolean) =
        runTest {
            underTest.setAskBeforeLargeDownloads(ask)
            verify(megaLocalStorageGateway).setAskBeforeLargeDownloads(ask)
        }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is ask before large downloads gets the value from the storage gateway`(expected: Boolean) =
        runTest {
            whenever(megaLocalStorageGateway.isAskBeforeLargeDownloads()).thenReturn(expected)
            assertThat(underTest.isAskBeforeLargeDownloads()).isEqualTo(expected)
        }

    @ParameterizedTest
    @ValueSource(strings = ["sd:cardUri"])
    @NullSource
    fun `test that database value is returned when get download to Sd card uri is invoked`(
        expected: String?,
    ) = runTest {
        whenever(databaseHandler.sdCardUri).thenReturn(expected)
        assertThat(underTest.getDownloadToSdCardUri()).isEqualTo(expected)
    }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that megaLocalStorageGateway value is returned when isAskSetDownloadLocation is invoked`(
        expected: Boolean,
    ) =
        runTest {
            whenever(megaLocalStorageGateway.isAskSetDownloadLocation()).thenReturn(expected)
            assertThat(underTest.isAskSetDownloadLocation()).isEqualTo(expected)
        }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that megaLocalStorageGateway is set with correct value when setAskSetDownloadLocation is invoked`(
        expected: Boolean,
    ) =
        runTest {
            underTest.setAskSetDownloadLocation(expected)
            verify(megaLocalStorageGateway).setAskSetDownloadLocation(expected)
        }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that megaLocalStorageGateway value is returned when isStorageAskAlways is invoked`(
        expected: Boolean,
    ) =
        runTest {
            whenever(megaLocalStorageGateway.isStorageAskAlways()).thenReturn(expected)
            assertThat(underTest.isStorageAskAlways()).isEqualTo(expected)
        }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that megaLocalStorageGateway is set with correct value when setStorageAskAlways is invoked`(
        expected: Boolean,
    ) =
        runTest {
            underTest.setStorageAskAlways(expected)
            verify(megaLocalStorageGateway).setStorageAskAlways(expected)
        }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    @NullSource
    fun `test that correct value is returned when getRaiseToHandSuggestionPreference is invoked`(
        expected: Boolean?,
    ) =
        runTest {
            whenever(callsPreferencesGateway.getRaiseToHandSuggestionPreference()).thenReturn(
                expected
            )
            assertThat(underTest.isRaiseToHandSuggestionShown()).isEqualTo(expected)
        }

    @Test
    fun `test that callPreferenceGateway is set with correct value when setRaiseToHandSuggestionShown is invoked`() =
        runTest {
            underTest.setRaiseToHandSuggestionShown()
            verify(callsPreferencesGateway).setRaiseToHandSuggestionPreference()
        }

    @Test
    fun `test that data return from cache when fileVersionsOptionCache is not null and call getFileVersionsOption with forceRefresh false`() =
        runTest {
            val expectedFileVersionsOption = true
            whenever(fileVersionsOptionCache.get()).thenReturn(expectedFileVersionsOption)
            val actual = underTest.getFileVersionsOption(false)
            verify(fileVersionsOptionCache, times(0)).set(any())
            verify(megaApiGateway, times(0)).getFileVersionsOption(any())
            assertThat(expectedFileVersionsOption).isEqualTo(actual)
        }

    @Test
    fun `test that data return from sdk when fileVersionsOptionCache is not null and call getFileVersionsOption with forceRefresh true`() =
        runTest {
            val expectedFileVersionsOption = true
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(expectedFileVersionsOption)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(fileVersionsOptionCache.get()).thenReturn(expectedFileVersionsOption.not())
            whenever(megaApiGateway.getFileVersionsOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.getFileVersionsOption(true)
            verify(fileVersionsOptionCache, times(1)).set(expectedFileVersionsOption)
            verify(megaApiGateway, times(1)).getFileVersionsOption(any())
            assertThat(expectedFileVersionsOption).isEqualTo(actual)
        }

    @Test
    fun `test that data return from sdk when fileVersionsOptionCache is null and call getFileVersionsOption with forceRefresh false`() =
        runTest {
            val expectedFileVersionsOption = true
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(expectedFileVersionsOption)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(fileVersionsOptionCache.get()).thenReturn(null)
            whenever(megaApiGateway.getFileVersionsOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.getFileVersionsOption(false)
            verify(fileVersionsOptionCache, times(1)).set(expectedFileVersionsOption)
            verify(megaApiGateway, times(1)).getFileVersionsOption(any())
            assertThat(expectedFileVersionsOption).isEqualTo(actual)
        }

    @Test
    fun `test that data return from sdk when fileVersionsOptionCache is API_ENOENT and call getFileVersionsOption with forceRefresh true`() =
        runTest {
            val expectedFileVersionsOption = false
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(expectedFileVersionsOption)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ENOENT)
            }
            whenever(fileVersionsOptionCache.get()).thenReturn(null)
            whenever(megaApiGateway.getFileVersionsOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.getFileVersionsOption(true)
            verify(fileVersionsOptionCache, times(1)).set(expectedFileVersionsOption)
            verify(megaApiGateway, times(1)).getFileVersionsOption(any())
            assertThat(expectedFileVersionsOption).isEqualTo(actual)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test enableGeoTagging calls uiPreferencesGateway enableGeoTagging`(enabled: Boolean) =
        runTest {
            underTest.enableGeoTagging(enabled)
            verify(uiPreferencesGateway).enableGeoTagging(enabled)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test monitorGeoTaggingStatus returns flow from uiPreferencesGateway`(enabled: Boolean) =
        runTest {
            whenever(uiPreferencesGateway.monitorGeoTaggingStatus()).thenReturn(flowOf(enabled))

            underTest.monitorGeoTaggingStatus().test {
                assertThat(awaitItem()).isEqualTo(enabled)
                awaitComplete()
            }
        }

    @Test
    fun `setRubbishBinAutopurgePeriod completes successfully`() = runTest {
        val days = 30
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest>()
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.setRubbishBinAutopurgePeriod(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        underTest.setRubbishBinAutopurgePeriod(days)
    }

    @Test
    fun `getRubbishBinAutopurgePeriod returns correct period when API_OK`() = runTest {
        val expectedDays = 30
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest> {
            on { number }.thenReturn(expectedDays.toLong())
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.getRubbishBinAutopurgePeriod(any())).thenAnswer {
            (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        val actualDays = underTest.getRubbishBinAutopurgePeriod()
        assertThat(actualDays).isEqualTo(expectedDays)
    }

    @Test
    fun `getRubbishBinAutopurgePeriod returns default period for free account when API_ENOENT`() =
        runTest {
            val expectedDays = 30
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ENOENT)
            }

            whenever(myAccountInfoFacade.accountTypeId).thenReturn(MegaAccountDetails.ACCOUNT_TYPE_FREE)
            whenever(megaApiGateway.getRubbishBinAutopurgePeriod(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            val actualDays = underTest.getRubbishBinAutopurgePeriod()
            assertThat(actualDays).isEqualTo(expectedDays)
        }

    @Test
    fun `getRubbishBinAutopurgePeriod returns default period for pro account when API_ENOENT`() =
        runTest {
            val expectedDays = 90
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ENOENT)
            }

            whenever(myAccountInfoFacade.accountTypeId).thenReturn(MegaAccountDetails.ACCOUNT_TYPE_PROI)
            whenever(megaApiGateway.getRubbishBinAutopurgePeriod(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            val actualDays = underTest.getRubbishBinAutopurgePeriod()
            assertThat(actualDays).isEqualTo(expectedDays)
        }

    @Test
    fun `getRubbishBinAutopurgePeriod throws exception when API error occurs`() = runTest {
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest>()
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EFAILED)
        }

        whenever(megaApiGateway.getRubbishBinAutopurgePeriod(any())).thenAnswer {
            (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        assertThrows<MegaException> {
            underTest.getRubbishBinAutopurgePeriod()
        }
    }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isRubbishBinAutopurgeEnabled returns the correct value from the gateway`(
        expected: Boolean,
    ) = runTest {
        whenever(megaApiGateway.serverSideRubbishBinAutopurgeEnabled()).thenReturn(expected)
        assertThat(underTest.isRubbishBinAutopurgeEnabled()).isEqualTo(expected)
    }
}
