package mega.privacy.android.data.repository

import android.content.Context
import android.content.Intent
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.R
import mega.privacy.android.data.gateway.AppInfoGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppInfoPreferencesGateway
import mega.privacy.android.data.mapper.environment.DevicePowerConnectionStateMapper
import mega.privacy.android.data.mapper.environment.ThermalStateMapper
import mega.privacy.android.data.wrapper.ApplicationIpAddressWrapper
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.entity.environment.ThermalState
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EnvironmentRepositoryImplTest {
    private lateinit var underTest: EnvironmentRepository

    private val deviceGateway = mock<DeviceGateway>()

    private val context = mock<Context>()

    private val megaApiGateway = mock<MegaApiGateway>()
    private val appInfoGateway = mock<AppInfoGateway>()
    private val appInfoPreferencesGateway = mock<AppInfoPreferencesGateway>()
    private val applicationIpAddressWrapper = mock<ApplicationIpAddressWrapper>()
    private val thermalStateMapper = mock<ThermalStateMapper>()
    private val devicePowerConnectionStateMapper = mock<DevicePowerConnectionStateMapper>()

    @BeforeAll
    fun setUp() {
        underTest = EnvironmentRepositoryImpl(
            deviceGateway = deviceGateway,
            context = context,
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            appInfoGateway = appInfoGateway,
            appInfoPreferencesGateway = appInfoPreferencesGateway,
            applicationIpAddressWrapper = applicationIpAddressWrapper,
            thermalStateMapper = thermalStateMapper,
            devicePowerConnectionStateMapper = devicePowerConnectionStateMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            deviceGateway,
            context,
            megaApiGateway,
            appInfoGateway,
            appInfoPreferencesGateway,
            applicationIpAddressWrapper,
            thermalStateMapper,
            devicePowerConnectionStateMapper,
        )
        whenever(deviceGateway.getCurrentDeviceLanguage()).thenReturn("en")
    }

    @Test
    fun `test that device manufacturer and model are included in device name`() = runTest {
        val manufacturer = "manufacturer"
        val model = "model"
        whenever(deviceGateway.getManufacturerName()).thenReturn(manufacturer)
        whenever(deviceGateway.getDeviceModel()).thenReturn(model)

        val (device, _) = underTest.getDeviceInfo()

        assertThat(device).isEqualTo("$manufacturer $model")
    }

    @Test
    fun `test that htc is capitalised`() = runTest {
        val manufacturer = "htc"
        val model = "model"
        whenever(deviceGateway.getManufacturerName()).thenReturn(manufacturer.lowercase())
        whenever(deviceGateway.getDeviceModel()).thenReturn(model)

        val (device, _) = underTest.getDeviceInfo()

        assertThat(device).isEqualTo("${manufacturer.uppercase()} $model")
    }

    @Test
    fun `test that application information is returned`() = runTest {
        val expectedVersion = "expectedVersion"
        val expectedSdkVersion = "expectedSdkVersion"

        whenever(context.getString(R.string.app_version)).thenReturn(expectedVersion)
        whenever(megaApiGateway.getSdkVersion()).thenReturn(expectedSdkVersion)

        val (appVersion, sdkVersion) = underTest.getAppInfo()
        assertThat(appVersion).isEqualTo(expectedVersion)
        assertThat(sdkVersion).isEqualTo(expectedSdkVersion)
    }

    @Test
    fun `test that thermal state is correctly returned`() =
        runTest {
            val expected = ThermalState.ThermalStateCritical
            val thermalStatus = 1
            whenever(deviceGateway.monitorThermalState).thenReturn(
                flowOf(thermalStatus)
            )
            whenever(thermalStateMapper(thermalStatus)).thenReturn(expected)
            underTest.monitorThermalState().test {
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "device charging: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that charging state is correctly returned`(
        isCharging: Boolean,
    ) = runTest {
        whenever(deviceGateway.monitorBatteryInfo).thenReturn(
            flowOf(BatteryInfo(100, isCharging))
        )
        underTest.monitorBatteryInfo().test {
            assertThat(awaitItem().isCharging).isEqualTo(isCharging)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that battery level is correctly returned`() = runTest {
        val batteryLevel = 100
        whenever(deviceGateway.monitorBatteryInfo).thenReturn(
            flowOf(BatteryInfo(batteryLevel, true))
        )
        underTest.monitorBatteryInfo().test {
            assertThat(awaitItem().level).isEqualTo(batteryLevel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the correct device power connection state is returned`() = runTest {
        val expectedState = DevicePowerConnectionState.Connected
        val powerType = Intent.ACTION_POWER_CONNECTED
        whenever(devicePowerConnectionStateMapper(powerType)).thenReturn(expectedState)
        whenever(deviceGateway.monitorDevicePowerConnectionState).thenReturn(
            flowOf(powerType)
        )

        underTest.monitorDevicePowerConnectionState().test {
            assertThat(awaitItem()).isEqualTo(expectedState)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
