package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.entity.DeviceInfo
import mega.privacy.android.domain.entity.SupportTicket
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultCreateSupportTicketTest {
    private lateinit var underTest: CreateSupportTicket
    private val deviceRepository = mock<EnvironmentRepository>()
    private val getAccountDetails = mock<GetAccountDetails>()

    private val device = "device"
    private val languageCode = "languageCode"
    private val appVersion = "appVersion"
    private val sdkVersion = "sdkVersion"
    private val accountEmail = "accountEmail"
    private val accountTypeString = "accountTypeString"
    private val fileName = "123-fileName.zip"
    private val description = "Issue description"

    @Before
    fun setUp() {
        whenever(deviceRepository.getDeviceInfo()).thenReturn(
            DeviceInfo(
                device = device,
                language = languageCode
            )
        )

        runBlocking {
            whenever(deviceRepository.getAppInfo()).thenReturn(
                AppInfo(
                    appVersion = appVersion,
                    sdkVersion = sdkVersion
                )
            )
        }

        runBlocking {
            whenever(getAccountDetails(false)).thenReturn(
                UserAccount(
                    userId = UserId(1L),
                    email = accountEmail,
                    isBusinessAccount = true,
                    isMasterBusinessAccount = true,
                    accountTypeIdentifier = 0,
                    accountTypeString = accountTypeString,
                )
            )
        }

        underTest = DefaultCreateSupportTicket(
            environmentRepository = deviceRepository,
            getAccountDetails = getAccountDetails
        )
    }

    @Test
    fun `test that device and app info is retrieved`() = runTest {
        underTest(description = description, null)

        verify(deviceRepository).getDeviceInfo()
        verify(deviceRepository).getAppInfo()
    }

    @Test
    fun `test that account information is retrieved`() = runTest {
        underTest(description = description, null)
        verify(getAccountDetails).invoke(false)
    }

    @Test
    fun `test that expected ticket is returned`() = runTest {
        val expected = SupportTicket(
            androidAppVersion = appVersion,
            sdkVersion = sdkVersion,
            device = device,
            accountType = accountTypeString,
            accountEmail = accountEmail,
            currentLanguage = languageCode,
            description = description,
            logFileName = fileName,
        )

        val actual = underTest(description, fileName)

        assertThat(actual).isEqualTo(expected)
    }
}