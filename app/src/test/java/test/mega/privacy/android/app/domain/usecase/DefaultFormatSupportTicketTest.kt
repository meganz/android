package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.domain.entity.SupportTicket
import mega.privacy.android.app.domain.usecase.DefaultFormatSupportTicket
import org.junit.Test

class DefaultFormatSupportTicketTest {
    private val underTest = DefaultFormatSupportTicket()

    private val expectedAndroidAppVersion = "expectedAndroidAppVersion"
    private val expectedSdkVersion = "expectedSdkVersion"
    private val expectedDevice = "expectedDevice"
    private val expectedCurrentLanguage = "expectedCurrentLanguage"
    private val expectedAccountEmail = "expectedAccountEmail"
    private val expectedAccountType = "expectedAccountType"
    private val expectedDescription = "expectedDescription"
    private val expectedLogFileName = "expectedLogFileName"

    @Test
    fun `test that format matches if no logs are present`() {
        val actual = underTest.invoke(getTicket(withLog = false))

        assertThat(actual).isEqualTo(expectedFormatNoLogs)
    }

    @Test
    fun `test that format matches if logs are present`() {
        val actual = underTest.invoke(getTicket(withLog = true))

        assertThat(actual).isEqualTo(expectedFormat)
    }

    private fun getTicket(withLog: Boolean): SupportTicket {
        return SupportTicket(
            androidAppVersion = expectedAndroidAppVersion,
            sdkVersion = expectedSdkVersion,
            device = expectedDevice,
            currentLanguage = expectedCurrentLanguage,
            accountEmail = expectedAccountEmail,
            accountType = expectedAccountType,
            description = expectedDescription,
            logFileName = if (withLog) expectedLogFileName else null,
        )
    }

    private val expectedFormat: String
        get() = createExpectedFormat(expectedLogFileName)

    private val expectedFormatNoLogs: String
        get() = createExpectedFormat("No log file")

    private fun createExpectedFormat(filename: String) = """
                $expectedDescription
                
                Report filename: $filename
                            
                Account Information:
                Email: $expectedAccountEmail
                Type: $expectedAccountType
                
                AppInformation:
                App name: Mega
                App version: $expectedAndroidAppVersion
                Sdk version: $expectedSdkVersion
                
                Device Information:
                Device: $expectedDevice
                Language: $expectedCurrentLanguage
            """.trimIndent()
}