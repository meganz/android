package mega.privacy.android.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CameraTimestampsPreferenceGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

/**
 * Test class for [DefaultSettingsRepository]
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalCoroutinesApi
internal class DefaultSettingsRepositoryTest {
    private lateinit var underTest: DefaultSettingsRepository

    private val databaseHandler: DatabaseHandler = mock {
        on { preferences }.thenReturn(mock())
    }
    private val context: Context = mock()
    private val apiFacade: MegaApiGateway = mock()
    private val megaLocalStorageGateway: MegaLocalStorageGateway = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val chatPreferencesGateway: ChatPreferencesGateway = mock()
    private val callsPreferencesGateway: CallsPreferencesGateway = mock()
    private val appPreferencesGateway: AppPreferencesGateway = mock()
    private val cacheFolderGateway: CacheFolderGateway = mock()
    private val uiPreferencesGateway: UIPreferencesGateway = mock()
    private val startScreenMapper: StartScreenMapper = mock()
    private val cameraTimestampsPreferenceGateway: CameraTimestampsPreferenceGateway = mock()
    private val fileManagementPreferencesGateway: FileManagementPreferencesGateway = mock()

    @Before
    fun setUp() {
        underTest = DefaultSettingsRepository(
            databaseHandler = databaseHandler,
            context = context,
            megaApiGateway = apiFacade,
            megaLocalStorageGateway = megaLocalStorageGateway,
            ioDispatcher = ioDispatcher,
            chatPreferencesGateway = chatPreferencesGateway,
            callsPreferencesGateway = callsPreferencesGateway,
            appPreferencesGateway = appPreferencesGateway,
            cacheFolderGateway = cacheFolderGateway,
            uiPreferencesGateway = uiPreferencesGateway,
            startScreenMapper = startScreenMapper,
            cameraTimestampsPreferenceGateway = cameraTimestampsPreferenceGateway,
            fileManagementPreferencesGateway = fileManagementPreferencesGateway,
        )
    }

    @Test
    fun `test that setFileVersionsOption success if no error is thrown`() = runTest {
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest>()
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(apiFacade.setFileVersionsOption(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        underTest.enableFileVersionsOption(true)
    }

    @Test(expected = MegaException::class)
    fun `test that calling setFileVersionsOption thrown an exception if the api does not return successfully`() =
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(apiFacade.setFileVersionsOption(any(), any())).thenAnswer {
                (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            underTest.enableFileVersionsOption(true)
        }

    @Test
    fun `test that setCamSyncWifi is invoked`() = runTest {
        underTest.setCamSyncWifi(wifiOnly = true)

        verify(megaLocalStorageGateway, times(1)).setCamSyncWifi(true)
    }
}