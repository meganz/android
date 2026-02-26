package mega.privacy.android.app.extensions

import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppendNoPlansQueryParamTest {

    @Test
    fun `test that mega io terms gets noplans param appended`() {
        assertThat(appendNoPlansParam("https://mega.io/terms".toUri()).toString())
            .isEqualTo("https://mega.io/terms?noplans=1")
    }

    @Test
    fun `test that mega io privacy gets noplans param appended`() {
        assertThat(appendNoPlansParam("https://mega.io/privacy".toUri()).toString())
            .isEqualTo("https://mega.io/privacy?noplans=1")
    }

    @Test
    fun `test that www mega io gets noplans param appended`() {
        assertThat(appendNoPlansParam("https://www.mega.io/chatandmeetings".toUri()).toString())
            .isEqualTo("https://www.mega.io/chatandmeetings?noplans=1")
    }

    @Test
    fun `test that help mega io gets noplans param appended`() {
        assertThat(appendNoPlansParam("https://help.mega.io/mobile-apps".toUri()).toString())
            .isEqualTo("https://help.mega.io/mobile-apps?noplans=1")
    }

    @Test
    fun `test that www help mega io gets noplans param appended`() {
        assertThat(appendNoPlansParam("https://www.help.mega.io/accounts/login-issues".toUri()).toString())
            .isEqualTo("https://www.help.mega.io/accounts/login-issues?noplans=1")
    }

    @Test
    fun `test that mega co nz legacy domain gets noplans param appended`() {
        assertThat(appendNoPlansParam("https://mega.co.nz/some-page".toUri()).toString())
            .isEqualTo("https://mega.co.nz/some-page?noplans=1")
    }

    @Test
    fun `test that www mega co nz gets noplans param appended`() {
        assertThat(appendNoPlansParam("https://www.mega.co.nz/info".toUri()).toString())
            .isEqualTo("https://www.mega.co.nz/info?noplans=1")
    }

    @Test
    fun `test that ampersand is used when URL has existing query params`() {
        assertThat(appendNoPlansParam("https://mega.io/page?foo=bar".toUri()).toString())
            .isEqualTo("https://mega.io/page?foo=bar&noplans=1")
    }

    @Test
    fun `test that ampersand is used when URL has multiple existing params`() {
        assertThat(appendNoPlansParam("https://help.mega.io/page?a=1&b=2".toUri()).toString())
            .isEqualTo("https://help.mega.io/page?a=1&b=2&noplans=1")
    }

    @Test
    fun `test that param is appended before fragment`() {
        assertThat(appendNoPlansParam("https://mega.io/page#section".toUri()).toString())
            .isEqualTo("https://mega.io/page?noplans=1#section")
    }

    @Test
    fun `test that param is appended before fragment when URL has existing query`() {
        assertThat(appendNoPlansParam("https://mega.io/page?foo=bar#section".toUri()).toString())
            .isEqualTo("https://mega.io/page?foo=bar&noplans=1#section")
    }

    @Test
    fun `test that github URL returns unchanged`() {
        val uri = "https://github.com/meganz/android".toUri()
        assertThat(appendNoPlansParam(uri)).isEqualTo(uri)
    }

    @Test
    fun `test that mega nz deeplink domain returns unchanged`() {
        val uri = "https://mega.nz/file/abc".toUri()
        assertThat(appendNoPlansParam(uri)).isEqualTo(uri)
    }

    @Test
    fun `test that mega app deeplink domain returns unchanged`() {
        val uri = "https://mega.app/chat/123".toUri()
        assertThat(appendNoPlansParam(uri)).isEqualTo(uri)
    }

    @Test
    fun `test that play store URL returns unchanged`() {
        val uri = "https://play.google.com/store/account/subscriptions".toUri()
        assertThat(appendNoPlansParam(uri)).isEqualTo(uri)
    }

    @Test
    fun `test that market scheme URL returns unchanged`() {
        val uri = "market://details?id=mega.privacy.android.app".toUri()
        assertThat(appendNoPlansParam(uri)).isEqualTo(uri)
    }

    @Test
    fun `test that URI with no host returns unchanged`() {
        val uri = "not-a-valid-url".toUri()
        assertThat(appendNoPlansParam(uri)).isEqualTo(uri)
    }

    @Test
    fun `test that URL with only fragment is handled correctly`() {
        assertThat(appendNoPlansParam("https://mega.io/#anchor".toUri()).toString())
            .isEqualTo("https://mega.io/?noplans=1#anchor")
    }
}
