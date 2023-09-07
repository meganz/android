package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.backup.BackupMapper
import mega.privacy.android.domain.entity.backup.Backup
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [BackupRepositoryImpl]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupRepositoryImplTest {
    private lateinit var underTest: BackupRepositoryImpl

    private val megaApiGateway = mock<MegaApiGateway>()
    private val backupMapper = mock<BackupMapper>()


    @BeforeAll
    fun setUp() {
        underTest = BackupRepositoryImpl(
            backupMapper = backupMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            backupMapper,
            megaApiGateway
        )
    }

    @Test
    fun `test that set backup info returns backup when the sdk returns success`() =
        runTest {
            val backup = mock<Backup>()
            val backupType = 1
            val targetNode = 1L
            val localFolder = "folderName"
            val backupName = "backupName"
            val state = 2
            val subState = 3
            val megaRequest = mock<MegaRequest>()
            val megaResponse = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
                on { errorString }.thenReturn("")
            }
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
                    subState,
                )
            ).isEqualTo(backup)
        }

    @ParameterizedTest(name = "returns {0}")
    @NullAndEmptySource
    @ValueSource(strings = ["Samsung Galaxy S23"])
    fun `test that get device name returns device name when the sdk returns success`(deviceName: String?) =
        runTest {
            val megaRequest = mock<MegaRequest>() {
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
}
