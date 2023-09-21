package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.backup.BackupDeviceNamesMapper
import mega.privacy.android.data.mapper.backup.BackupInfoListMapper
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.data.mapper.backup.BackupMapper
import mega.privacy.android.data.mapper.camerauploads.BackupStateIntMapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaBackupInfoList
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaStringMap
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [BackupRepositoryImpl]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupRepositoryImplTest {
    private lateinit var underTest: BackupRepositoryImpl

    private val backupDeviceNamesMapper = mock<BackupDeviceNamesMapper>()
    private val backupMapper = mock<BackupMapper>()
    private val backupInfoListMapper = mock<BackupInfoListMapper>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val appEventGateway = mock<AppEventGateway>()
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val backupInfoTypeIntMapper = mock<BackupInfoTypeIntMapper>()
    private val backupStateIntMapper = mock<BackupStateIntMapper>()

    private val deviceId = "12345-6789"
    private val deviceName = "New Device Name"

    @BeforeAll
    fun setUp() {
        underTest = BackupRepositoryImpl(
            backupDeviceNamesMapper = backupDeviceNamesMapper,
            backupInfoListMapper = backupInfoListMapper,
            backupMapper = backupMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
            appEventGateway = appEventGateway,
            megaLocalStorageGateway = megaLocalStorageGateway,
            backupInfoTypeIntMapper = backupInfoTypeIntMapper,
            backupStateIntMapper = backupStateIntMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            backupInfoListMapper,
            backupMapper,
            megaApiGateway,
        )
    }

    @Test
    fun `test that set backup info returns backup when the sdk returns success`() =
        runTest {
            val backup = mock<Backup>()
            val backupType = BackupInfoType.CAMERA_UPLOADS
            val targetNode = 1L
            val localFolder = "folderName"
            val backupName = "backupName"
            val state = BackupState.ACTIVE
            val megaRequest = mock<MegaRequest>()
            val megaResponse = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
                on { errorString }.thenReturn("")
            }
            whenever(backupInfoTypeIntMapper(backupType)).thenReturn(1)
            whenever(backupStateIntMapper(state)).thenReturn(2)
            whenever(
                megaApiGateway.setBackup(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            ).thenAnswer {
                ((it.arguments[6] as MegaRequestListenerInterface)).onRequestFinish(
                    api = mock(),
                    request = megaRequest,
                    e = megaResponse,
                )
            }

            whenever(backupMapper(megaRequest)).thenReturn(backup)
            assertThat(
                underTest.setBackup(
                    backupType,
                    targetNode,
                    localFolder,
                    backupName,
                    state,
                )
            ).isEqualTo(backup)
        }

    @ParameterizedTest(name = "deviceId: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = ["12345-6789"])
    fun `test that get device id returns the current device id`(deviceId: String?) = runTest {
        whenever(megaApiGateway.getDeviceId()).thenReturn(deviceId)
        assertThat(underTest.getDeviceId()).isEqualTo(deviceId)
    }

    @Test
    fun `test that get device id and name map returns the pairings when the sdk returns success`() =
        runTest {
            val testMegaStringMap = mock<MegaStringMap>()
            val actualMap = mapOf(
                "12345" to "Device One",
                "67890" to "Device Two",
            )
            setGetDeviceIdAndNameMapData(
                megaErrorCode = MegaError.API_OK,
                megaErrorString = "",
                testMegaStringMap = testMegaStringMap
            )
            whenever(backupDeviceNamesMapper(testMegaStringMap)).thenReturn(actualMap)
            assertThat(underTest.getDeviceIdAndNameMap()).isEqualTo(actualMap)
        }

    @Test
    fun `test that get device id and name map throws a mega exception when the sdk returns an error`() =
        runTest {
            val testMegaStringMap = mock<MegaStringMap>()
            setGetDeviceIdAndNameMapData(
                megaErrorCode = MegaError.API_EFAILED,
                megaErrorString = "Error Message",
                testMegaStringMap = testMegaStringMap,
            )

            verifyNoInteractions(backupDeviceNamesMapper)
            assertThrows<MegaException> { underTest.getDeviceIdAndNameMap() }
        }

    /**
     * Sets up the data for the Get Device ID and Name Map Test
     *
     * @param megaErrorCode The expected [MegaError]
     * @param megaErrorString The expected message from [MegaError]
     * @param testMegaStringMap The expected [MegaStringMap]
     */
    private fun setGetDeviceIdAndNameMapData(
        megaErrorCode: Int,
        megaErrorString: String,
        testMegaStringMap: MegaStringMap,
    ) {
        val megaRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { megaStringMap }.thenReturn(testMegaStringMap)
        }
        val megaResponse = mock<MegaError> {
            on { errorCode }.thenReturn(megaErrorCode)
            on { errorString }.thenReturn(megaErrorString)
        }

        whenever(
            megaApiGateway.getUserAttribute(
                attributeIdentifier = Mockito.eq(MegaApiJava.USER_ATTR_DEVICE_NAMES),
                listener = any(),
            )
        ).thenAnswer {
            ((it.arguments[1] as MegaRequestListenerInterface)).onRequestFinish(
                api = mock(),
                request = megaRequest,
                e = megaResponse,
            )
        }
    }

    @ParameterizedTest(name = "returns {0}")
    @NullAndEmptySource
    @ValueSource(strings = ["Samsung Galaxy S23"])
    fun `test that get device name returns device name when the sdk returns success`(deviceName: String?) =
        runTest {
            val megaRequest = mock<MegaRequest> {
                on { name }.thenReturn(deviceName)
            }
            val megaResponse = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
                on { errorString }.thenReturn("")
            }
            whenever(
                megaApiGateway.getDeviceName(
                    any(),
                    any(),
                )
            ).thenAnswer {
                ((it.arguments[1] as MegaRequestListenerInterface)).onRequestFinish(
                    api = mock(),
                    request = megaRequest,
                    e = megaResponse,
                )
            }
            assertThat(underTest.getDeviceName("1234")).isEqualTo(deviceName)
        }

    @Test
    fun `test that rename device is successful when the sdk returns success`() = runTest {
        setRenameDeviceData(megaErrorCode = MegaError.API_OK, megaErrorString = "")
        assertDoesNotThrow { underTest.renameDevice(deviceId = deviceId, deviceName = deviceName) }
    }

    @Test
    fun `test that rename device throws a mega exception when the sdk returns an error`() =
        runTest {
            setRenameDeviceData(
                megaErrorCode = MegaError.API_EFAILED,
                megaErrorString = "Error Message",
            )
            assertThrows<MegaException> {
                underTest.renameDevice(
                    deviceId = deviceId,
                    deviceName = deviceName,
                )
            }
        }

    /**
     * Sets up the data for the Rename Device Test
     *
     * @param megaErrorCode The expected [MegaError]
     * @param megaErrorString The expected message from [MegaError]
     */
    private fun setRenameDeviceData(megaErrorCode: Int, megaErrorString: String) {
        val megaRequest =
            mock<MegaRequest> { on { type }.thenReturn(MegaRequest.TYPE_SET_ATTR_USER) }
        val megaResponse = mock<MegaError> {
            on { errorCode }.thenReturn(megaErrorCode)
            on { errorString }.thenReturn(megaErrorString)
        }

        whenever(
            megaApiGateway.setDeviceName(
                deviceId = any(),
                deviceName = any(),
                listener = any(),
            )
        ).thenAnswer {
            ((it.arguments[2] as MegaRequestListenerInterface)).onRequestFinish(
                api = mock(),
                request = megaRequest,
                e = megaResponse,
            )
        }
    }

    @Test
    fun `test that get backup info returns all backup information of the user when the sdk returns success`() =
        runTest {
            val testMegaBackupInfoList = mock<MegaBackupInfoList>()
            val testBackupInfoList = listOf(mock<BackupInfo>())
            setGetBackupInfoData(
                megaErrorCode = MegaError.API_OK,
                megaErrorString = "",
                testMegaBackupInfoList = testMegaBackupInfoList,
            )

            whenever(backupInfoListMapper(testMegaBackupInfoList)).thenReturn(testBackupInfoList)
            assertThat(underTest.getBackupInfo()).isEqualTo(testBackupInfoList)
        }

    @Test
    fun `test that get backup info throws an exception when the sdk returns an error`() = runTest {
        val testMegaBackupInfoList = mock<MegaBackupInfoList>()
        setGetBackupInfoData(
            megaErrorCode = MegaError.API_EFAILED,
            megaErrorString = "Error Message",
            testMegaBackupInfoList = testMegaBackupInfoList,
        )

        verifyNoInteractions(backupInfoListMapper)
        assertThrows<MegaException> { underTest.getBackupInfo() }
    }

    /**
     * Sets up the data for the Get Backup Info Test
     *
     * @param megaErrorCode The expected [MegaError]
     * @param megaErrorString The expected message from [MegaError]
     * @param testMegaBackupInfoList The expected [MegaBackupInfoList]
     */
    private fun setGetBackupInfoData(
        megaErrorCode: Int,
        megaErrorString: String,
        testMegaBackupInfoList: MegaBackupInfoList,
    ) {
        val megaRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_BACKUP_INFO)
            on { megaBackupInfoList }.thenReturn(testMegaBackupInfoList)
        }
        val megaResponse = mock<MegaError> {
            on { errorCode }.thenReturn(megaErrorCode)
            on { errorString }.thenReturn(megaErrorString)
        }
        whenever(megaApiGateway.getBackupInfo(any())).thenAnswer {
            ((it.arguments[0] as MegaRequestListenerInterface)).onRequestFinish(
                api = mock(),
                request = megaRequest,
                e = megaResponse,
            )
        }
    }
}
