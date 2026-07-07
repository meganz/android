package mega.privacy.android.data.repository

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.mapper.AppVersionMapper
import mega.privacy.android.data.preferences.PinnedItemsSortPreferenceDataStore
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.entity.home.PinnedHomeItem
import mega.privacy.android.domain.entity.home.PinnedHomeItemsSortField
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
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
    private val getAccountTypeUseCase: GetAccountTypeUseCase = mock()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val appVersionMapper = mock<AppVersionMapper>()
    private val pinnedItemsSortPreferenceDataStore = mock<PinnedItemsSortPreferenceDataStore>()

    @BeforeAll
    fun setUp() {
        underTest = DefaultSettingsRepository(
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
            getAccountTypeUseCase = getAccountTypeUseCase,
            megaLocalRoomGateway = megaLocalRoomGateway,
            appVersionMapper = appVersionMapper,
            pinnedItemsSortPreferenceDataStore = pinnedItemsSortPreferenceDataStore,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
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
            megaLocalRoomGateway,
            appVersionMapper,
            pinnedItemsSortPreferenceDataStore,
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

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that megaLocalStorageGateway value is returned when isShouldPromptToSaveDestination is invoked`(
        expected: Boolean,
    ) =
        runTest {
            whenever(megaLocalStorageGateway.isShouldPromptToSaveDestination()).thenReturn(expected)
            assertThat(underTest.isShouldPromptToSaveDestination()).isEqualTo(expected)
        }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that megaLocalStorageGateway is set with correct value when setShouldPromptToSaveDestination is invoked`(
        expected: Boolean,
    ) =
        runTest {
            underTest.setShouldPromptToSaveDestination(expected)
            verify(megaLocalStorageGateway).setShouldPromptToSaveDestination(expected)
        }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that megaLocalStorageGateway value is returned when isAskForDownloadLocation is invoked`(
        expected: Boolean,
    ) =
        runTest {
            whenever(megaLocalStorageGateway.isAskForDownloadLocation()).thenReturn(expected)
            assertThat(underTest.isAskForDownloadLocation()).isEqualTo(expected)
        }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that megaLocalStorageGateway is set with correct value when setAskForDownloadLocation is invoked`(
        expected: Boolean,
    ) =
        runTest {
            underTest.setAskForDownloadLocation(expected)
            verify(megaLocalStorageGateway).setAskForDownloadLocation(expected)
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

            whenever(getAccountTypeUseCase()).thenReturn(AccountType.FREE)
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

            whenever(getAccountTypeUseCase()).thenReturn(AccountType.PRO_I)
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

    @Test
    fun `test that value passed to set auto accept is passed on to the gateway`() = runTest {
        val expected = true
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }
        val request = mock<MegaRequest> {
            on { flag }.thenReturn(expected)
            on { type }.thenReturn(MegaRequest.TYPE_SET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION)
        }
        val api = mock<MegaApiJava>()
        whenever(megaApiGateway.setContactLinksOption(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                megaError
            )
        }

        underTest.setContactLinksOption(expected)
        verify(megaApiGateway).setContactLinksOption(eq(expected), any())
    }


    @Test
    fun `test that set start preference destination calls the gateway with the correct value`() =
        runTest {
            val expected = "test"
            underTest.setStartScreenPreferenceDestination(
                StartScreenDestinationPreference(
                    expected
                )
            )

            verify(uiPreferencesGateway).setSerialisedStartScreenPreferenceDestination(expected)
        }

    @Test
    fun `test that monitorStartScreenPreferenceDestination returns the correct value from the gateway`() =
        runTest {
            val serialisedDestination = "test"
            val expected = StartScreenDestinationPreference(serialisedDestination)
            whenever(uiPreferencesGateway.monitorSerialisedStartScreenPreferenceDestination()).thenReturn(
                flowOf(serialisedDestination)
            )

            underTest.monitorStartScreenPreferenceDestination().test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorAskBeforePreviewDownloads returns the value from the gateway`() =
        runTest {
            whenever(appPreferencesGateway.monitorBoolean(any(), any())).thenReturn(flowOf(false))

            underTest.monitorAskBeforePreviewDownloads().test {
                assertThat(awaitItem()).isFalse()
                awaitComplete()
            }
        }

    @Test
    fun `test that setAskBeforePreviewDownloads stores the value in the gateway`() = runTest {
        underTest.setAskBeforePreviewDownloads(false)

        verify(appPreferencesGateway).putBoolean(any(), eq(false))
    }

    @Test
    fun `test that monitorHomeScreenWidgetConfiguration returns the values from the gateway`() =
        runTest {
            val expected = listOf(mock<HomeWidgetConfiguration>())
            megaLocalRoomGateway.stub {
                on { monitorHomeScreenWidgetConfigurations() }.thenReturn(flow {
                    emit(expected)
                    awaitCancellation()
                }
                )
            }

            underTest.monitorHomeScreenWidgetConfiguration().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

    @Test
    fun `test that monitorPinnedHomeItems returns the values from the gateway`() =
        runTest {
            val expected = listOf(mock<PinnedHomeItem>())
            megaLocalRoomGateway.stub {
                on { monitorPinnedHomeItems() }.thenReturn(flow {
                    emit(expected)
                    awaitCancellation()
                }
                )
            }

            underTest.monitorPinnedHomeItems().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

    @Test
    fun `test that addPinnedHomeItems delegates to the gateway`() = runTest {
        val items = listOf(mock<PinnedHomeItem>())

        underTest.addPinnedHomeItems(items)

        verify(megaLocalRoomGateway).insertPinnedHomeItems(items)
    }

    @Test
    fun `test that removePinnedHomeItem delegates to the gateway with the node handle`() = runTest {
        underTest.removePinnedHomeItem(NodeId(123L))

        verify(megaLocalRoomGateway).deletePinnedHomeItem(123L)
    }

    @Test
    fun `test that updatePinnedHomeItemName delegates to the gateway with the node handle`() =
        runTest {
            underTest.updatePinnedHomeItemName(NodeId(123L), "Renamed")

            verify(megaLocalRoomGateway).updatePinnedHomeItemName(123L, "Renamed")
        }

    @Test
    fun `test that clearPinnedHomeItems delegates to the gateway`() = runTest {
        underTest.clearPinnedHomeItems()

        verify(megaLocalRoomGateway).deleteAllPinnedHomeItems()
    }

    @Test
    fun `test that monitorPinnedItemsSortPreference returns the values from the data store`() =
        runTest {
            val expected = PinnedHomeItemsSortField.Name to SortDirection.Ascending
            pinnedItemsSortPreferenceDataStore.stub {
                on { monitorSortPreference() }.thenReturn(flow {
                    emit(expected)
                    awaitCancellation()
                })
            }

            underTest.monitorPinnedItemsSortPreference().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

    @Test
    fun `test that setPinnedItemsSortPreference delegates to the data store`() = runTest {
        underTest.setPinnedItemsSortPreference(
            PinnedHomeItemsSortField.DateAdded,
            SortDirection.Descending,
        )

        verify(pinnedItemsSortPreferenceDataStore).setSortPreference(
            PinnedHomeItemsSortField.DateAdded,
            SortDirection.Descending,
        )
    }

    @ParameterizedTest(name = "flag: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that getContactLinksOption returns the request flag when the request succeeds`(
        flagValue: Boolean,
    ) = runTest {
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION)
            on { flag }.thenReturn(flagValue)
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.getContactLinksOption(any())).thenAnswer {
            (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        assertThat(underTest.getContactLinksOption()).isEqualTo(flagValue)
    }

    @Test
    fun `test that getContactLinksOption returns true when the option has never been set`() =
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
                on { paramType }.thenReturn(MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ENOENT)
            }

            whenever(megaApiGateway.getContactLinksOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            assertThat(underTest.getContactLinksOption()).isTrue()
        }

    @Test
    fun `test that getContactLinksOption throws an exception when the request fails`() = runTest {
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION)
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EFAILED)
        }

        whenever(megaApiGateway.getContactLinksOption(any())).thenAnswer {
            (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        assertThrows<MegaException> {
            underTest.getContactLinksOption()
        }
    }
}
