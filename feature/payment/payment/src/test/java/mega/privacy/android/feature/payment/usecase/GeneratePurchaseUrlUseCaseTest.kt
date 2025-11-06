package mega.privacy.android.feature.payment.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GeneratePurchaseUrlUseCaseTest {

    private lateinit var underTest: GeneratePurchaseUrlUseCase

    private val getDomainNameUseCase: GetDomainNameUseCase = mock()
    private val environmentRepository: EnvironmentRepository = mock()
    private val getSessionTransferURLUseCase: GetSessionTransferURLUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = GeneratePurchaseUrlUseCase(
            getDomainNameUseCase = getDomainNameUseCase,
            environmentRepository = environmentRepository,
            getSessionTransferURLUseCase = getSessionTransferURLUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getDomainNameUseCase, environmentRepository, getSessionTransferURLUseCase)
    }

    @Test
    fun `test that URL is generated with mega nz domain when domain use case returns mega nz`() =
        runTest {
            val productId = "pro1"
            val appVersion = "15.21"
            val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
            val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion"
            val sessionTransferUrl =
                "https://mega.nz/#$productId/uao=Android app Ver $appVersion?session=abc123"

            whenever(getDomainNameUseCase()).thenReturn("mega.nz")
            whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
            wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

            val result = underTest(productId, null)

            assertThat(result).isEqualTo(sessionTransferUrl)
            val urlCaptor = argumentCaptor<String>()
            verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
            assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
        }

    @Test
    fun `test that URL is generated with mega app domain when domain use case returns mega app`() =
        runTest {
            val productId = "pro2"
            val appVersion = "15.21"
            val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
            val inputUrl = "https://mega.app/$productId/uao=Android app Ver $appVersion"
            val sessionTransferUrl =
                "https://mega.app/#$productId/uao=Android app Ver $appVersion?session=xyz789"

            whenever(getDomainNameUseCase()).thenReturn("mega.app")
            whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
            wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

            val result = underTest(productId, null)

            assertThat(result).isEqualTo(sessionTransferUrl)
            val urlCaptor = argumentCaptor<String>()
            verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
            assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
        }

    @Test
    fun `test that URL includes months parameter when months is provided`() = runTest {
        val productId = "pro3"
        val months = 12
        val appVersion = "15.21"
        val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
        val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion&m=$months"
        val sessionTransferUrl =
            "https://mega.nz/#$productId/uao=Android app Ver $appVersion&m=$months?session=def456"

        whenever(getDomainNameUseCase()).thenReturn("mega.nz")
        whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
        wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

        val result = underTest(productId, months)

        assertThat(result).isEqualTo(sessionTransferUrl)
        val urlCaptor = argumentCaptor<String>()
        verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
        assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
    }

    @Test
    fun `test that URL does not include months parameter when months is null`() = runTest {
        val productId = "pro4"
        val appVersion = "15.21"
        val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
        val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion"
        val sessionTransferUrl =
            "https://mega.nz/#$productId/uao=Android app Ver $appVersion?session=ghi789"

        whenever(getDomainNameUseCase()).thenReturn("mega.nz")
        whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
        wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

        val result = underTest(productId, null)

        assertThat(result).isEqualTo(sessionTransferUrl)
        assertThat(result).doesNotContain("&m=")
        val urlCaptor = argumentCaptor<String>()
        verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
        assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
        assertThat(urlCaptor.firstValue).doesNotContain("&m=")
    }

    @Test
    fun `test that URL does not include months parameter when months is not provided`() =
        runTest {
            val productId = "pro5"
            val appVersion = "15.21"
            val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
            val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion"
            val sessionTransferUrl =
                "https://mega.nz/#$productId/uao=Android app Ver $appVersion?session=jkl012"

            whenever(getDomainNameUseCase()).thenReturn("mega.nz")
            whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
            wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

            val result = underTest(productId, null)

            assertThat(result).isEqualTo(sessionTransferUrl)
            assertThat(result).doesNotContain("&m=")
            val urlCaptor = argumentCaptor<String>()
            verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
            assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
            assertThat(urlCaptor.firstValue).doesNotContain("&m=")
        }

    @Test
    fun `test that URL includes months parameter with mega app domain when both are provided`() =
        runTest {
            val productId = "pro6"
            val months = 6
            val appVersion = "15.21"
            val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
            val inputUrl = "https://mega.app/$productId/uao=Android app Ver $appVersion&m=$months"
            val sessionTransferUrl =
                "https://mega.app/#$productId/uao=Android app Ver $appVersion&m=$months?session=mno345"

            whenever(getDomainNameUseCase()).thenReturn("mega.app")
            whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
            wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

            val result = underTest(productId, months)

            assertThat(result).isEqualTo(sessionTransferUrl)
            val urlCaptor = argumentCaptor<String>()
            verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
            assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
        }

    @Test
    fun `test that URL handles different product IDs correctly`() = runTest {
        val productId = "lite"
        val appVersion = "15.21"
        val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
        val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion"
        val sessionTransferUrl =
            "https://mega.nz/#$productId/uao=Android app Ver $appVersion?session=pqr678"

        whenever(getDomainNameUseCase()).thenReturn("mega.nz")
        whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
        wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

        val result = underTest(productId, null)

        assertThat(result).isEqualTo(sessionTransferUrl)
        assertThat(result).contains(productId)
        val urlCaptor = argumentCaptor<String>()
        verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
        assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
        assertThat(urlCaptor.firstValue).contains(productId)
    }

    @Test
    fun `test that URL handles different app versions correctly`() = runTest {
        val productId = "pro7"
        val appVersion = "16.0"
        val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
        val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion"
        val sessionTransferUrl =
            "https://mega.nz/#$productId/uao=Android app Ver $appVersion?session=stu901"

        whenever(getDomainNameUseCase()).thenReturn("mega.nz")
        whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
        wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

        val result = underTest(productId, null)

        assertThat(result).isEqualTo(sessionTransferUrl)
        assertThat(result).contains(appVersion)
        val urlCaptor = argumentCaptor<String>()
        verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
        assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
        assertThat(urlCaptor.firstValue).contains(appVersion)
    }

    @Test
    fun `test that URL handles zero months correctly`() = runTest {
        val productId = "pro8"
        val months = 0
        val appVersion = "15.21"
        val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
        val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion&m=$months"
        val sessionTransferUrl =
            "https://mega.nz/#$productId/uao=Android app Ver $appVersion&m=$months?session=vwx234"

        whenever(getDomainNameUseCase()).thenReturn("mega.nz")
        whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
        wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

        val result = underTest(productId, months)

        assertThat(result).isEqualTo(sessionTransferUrl)
        val urlCaptor = argumentCaptor<String>()
        verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
        assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
    }

    @Test
    fun `test that URL handles three part app version format like 15_21_1`() = runTest {
        val productId = "pro9"
        val appVersion = "15.21.1"
        val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
        val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion"
        val sessionTransferUrl =
            "https://mega.nz/#$productId/uao=Android app Ver $appVersion?session=yza567"

        whenever(getDomainNameUseCase()).thenReturn("mega.nz")
        whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
        wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

        val result = underTest(productId, null)

        assertThat(result).isEqualTo(sessionTransferUrl)
        assertThat(result).contains(appVersion)
        val urlCaptor = argumentCaptor<String>()
        verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
        assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
        assertThat(urlCaptor.firstValue).contains(appVersion)
    }

    @Test
    fun `test that URL handles three part app version format like 15_21_2`() = runTest {
        val productId = "pro10"
        val appVersion = "15.21.2"
        val appInfo = AppInfo(appVersion = appVersion, sdkVersion = null)
        val inputUrl = "https://mega.nz/$productId/uao=Android app Ver $appVersion"
        val sessionTransferUrl =
            "https://mega.nz/#$productId/uao=Android app Ver $appVersion?session=bcd890"

        whenever(getDomainNameUseCase()).thenReturn("mega.nz")
        whenever(environmentRepository.getAppInfo()).thenReturn(appInfo)
        wheneverBlocking { getSessionTransferURLUseCase(any()) }.thenReturn(sessionTransferUrl)

        val result = underTest(productId, null)

        assertThat(result).isEqualTo(sessionTransferUrl)
        assertThat(result).contains(appVersion)
        val urlCaptor = argumentCaptor<String>()
        verify(getSessionTransferURLUseCase).invoke(urlCaptor.capture())
        assertThat(urlCaptor.firstValue).isEqualTo(inputUrl)
        assertThat(urlCaptor.firstValue).contains(appVersion)
    }
}
