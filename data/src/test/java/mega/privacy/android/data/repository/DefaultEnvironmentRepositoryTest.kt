package mega.privacy.android.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.R
import mega.privacy.android.data.gateway.AppInfoGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppInfoPreferencesGateway
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultEnvironmentRepositoryTest {
    private lateinit var underTest: EnvironmentRepository

    private val deviceGateway =
        mock<DeviceGateway> { on { getCurrentDeviceLanguage() }.thenReturn("en") }

    private val context = mock<Context>()

    private val megaApiGateway = mock<MegaApiGateway>()
    private val appInfoGateway = mock<AppInfoGateway>()
    private val appInfoPreferencesGateway = mock<AppInfoPreferencesGateway>()

    @Before
    fun setUp() {
        underTest = DefaultEnvironmentRepository(
            deviceGateway = deviceGateway,
            context = context,
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            appInfoGateway = appInfoGateway,
            appInfoPreferencesGateway = appInfoPreferencesGateway,
        )
    }

    @Test
    fun `test that device manufacturer and model are included in device name`() {
        val manufacturer = "manufacturer"
        val model = "model"
        whenever(deviceGateway.getManufacturerName()).thenReturn(manufacturer)
        whenever(deviceGateway.getDeviceModel()).thenReturn(model)

        val (device, _) = underTest.getDeviceInfo()

        assertThat(device).isEqualTo("$manufacturer $model")
    }

    @Test
    fun `test that htc is capitalised`() {
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
}