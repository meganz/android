package mega.privacy.android.feature.devicecenter.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.feature.devicecenter.domain.exception.SetDeviceNameException
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [DeviceCenterRepository]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceCenterRepositoryImplTest {
    private lateinit var underTest: DeviceCenterRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    private val deviceName = "Test Device Name"

    @BeforeAll
    fun setUp() {
        underTest = DeviceCenterRepositoryImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(megaApiGateway)
    }

    @Test
    fun `test that set device name is successful`() = runTest {
        setupSetDeviceNameData(megaErrorCode = MegaError.API_OK, megaErrorString = "")
        assertDoesNotThrow { underTest.setDeviceName(deviceName) }
    }

    @Test
    fun `test that set device name throws a name already exists exception`() = runTest {
        setupSetDeviceNameData(
            megaErrorCode = MegaError.API_EEXIST,
            megaErrorString = "Error Message",
        )
        assertThrows<SetDeviceNameException.NameAlreadyExists> { underTest.setDeviceName(deviceName) }
    }

    @Test
    fun `test that set device name throws an unknown exception when there is no matching mega error code`() =
        runTest {
            setupSetDeviceNameData(
                megaErrorCode = MegaError.API_EFAILED,
                megaErrorString = "Error Message",
            )
            assertThrows<SetDeviceNameException.Unknown> { underTest.setDeviceName(deviceName) }
        }

    /**
     * Sets up the data for Tests that verify [DeviceCenterRepository.setDeviceName]
     *
     * @param megaErrorCode The expected [MegaError]
     * @param megaErrorString The expected message from [MegaError]
     */
    private fun setupSetDeviceNameData(megaErrorCode: Int, megaErrorString: String) {
        val megaRequest =
            mock<MegaRequest> { on { type }.thenReturn(MegaRequest.TYPE_SET_ATTR_USER) }
        val megaResponse = mock<MegaError> {
            on { errorCode }.thenReturn(megaErrorCode)
            on { errorString }.thenReturn(megaErrorString)
        }

        whenever(megaApiGateway.setDeviceName(deviceName = any(), listener = any())).thenAnswer {
            ((it.arguments[1] as MegaRequestListenerInterface)).onRequestFinish(
                api = mock(),
                request = megaRequest,
                e = megaResponse,
            )
        }
    }

    @ParameterizedTest(name = "deviceId: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = ["12345-6789"])
    fun `test that get device id returns the current device id`(deviceId: String?) = runTest {
        whenever(megaApiGateway.getDeviceId()).thenReturn(deviceId)
        assertThat(underTest.getDeviceId()).isEqualTo(deviceId)
    }
}