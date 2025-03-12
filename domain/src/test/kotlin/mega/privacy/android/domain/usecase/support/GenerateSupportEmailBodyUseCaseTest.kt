import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.entity.DeviceInfo
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.usecase.support.GenerateSupportEmailBodyUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GenerateSupportEmailBodyUseCaseTest {

    private lateinit var generateSupportEmailBodyUseCase: GenerateSupportEmailBodyUseCase
    private val deviceRepository: EnvironmentRepository = mock()
    private val networkRepository: NetworkRepository = mock()

    @BeforeEach
    fun setUp() {
        generateSupportEmailBodyUseCase = GenerateSupportEmailBodyUseCase(deviceRepository, networkRepository)
    }

    @Test
    fun `test generate email body with valid input`() = runBlocking {
        val appInfo = AppInfo(appVersion = "1.0.0", sdkVersion = "123232.323232")
        val deviceInfo = DeviceInfo("Pixel 5", "en")
        val timezone = "GMT+0"
        val connectivityState = ConnectivityState.Connected(true)
        val deviceSdkVersionName = "Android 14.0"

        whenever(deviceRepository.getAppInfo()).thenReturn(appInfo)
        whenever(deviceRepository.getDeviceInfo()).thenReturn(deviceInfo)
        whenever(deviceRepository.getTimezone()).thenReturn(timezone)
        whenever(networkRepository.getCurrentConnectivityState()).thenReturn(connectivityState)
        whenever(deviceRepository.getDeviceSdkVersionName()).thenReturn(deviceSdkVersionName)

        val expectedEmailBody = """
            Please write your feedback here:










            App information:
            App Name: MEGA
            App Version: 1.0.0

            Device information:
            Device: Pixel 5
            Android version: Android 14.0
            Language: en
            Timezone: GMT+0
            Connection Status: WIFI

        """.trimIndent()

        val actualEmailBody = generateSupportEmailBodyUseCase.invoke()

        assertThat(actualEmailBody).isEqualTo(expectedEmailBody)
    }
}