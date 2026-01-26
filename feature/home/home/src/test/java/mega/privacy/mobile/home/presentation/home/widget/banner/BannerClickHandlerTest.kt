package mega.privacy.mobile.home.presentation.home.widget.banner

import android.content.Context
import android.content.Intent
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.WebSiteNavKey
import mega.privacy.mobile.analytics.event.PwmSmartBannerItemSelectedEvent
import mega.privacy.mobile.analytics.event.TransferItSmartBannerItemSelectedEvent
import mega.privacy.mobile.analytics.event.VpnSmartBannerItemSelectedEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.stream.Stream

/**
 * Unit tests for BannerClickHandler
 */
class BannerClickHandlerTest {

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()

        @JvmStatic
        fun vpnUrlsProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("https://vpn.mega.nz"),
            Arguments.of("https://vpn.mega.nz/"),
            Arguments.of("https://vpn.mega.nz/path"),
            Arguments.of("https://vpn.mega.nz?param=value"),
            Arguments.of("https://vpn.mega.app"),
            Arguments.of("https://vpn.mega.app/"),
            Arguments.of("https://vpn.mega.app/path"),
            Arguments.of("https://vpn.mega.app?param=value"),
            Arguments.of("https://vpn.mega.co.nz"),
            Arguments.of("https://vpn.mega.io"),
            Arguments.of("https://vpn.megaad.nz"),
            Arguments.of("https://vpn.mega.nz/login"),
            Arguments.of("https://vpn.mega.nz/dashboard?user=test"),
            Arguments.of("https://vpn.mega.co.nz/path/to/page"),
            // Path-based VPN URLs
            Arguments.of("https://mega.io/vpn"),
            Arguments.of("https://mega.nz/vpn"),
            Arguments.of("https://mega.co.nz/vpn"),
            Arguments.of("https://mega.app/vpn"),
            Arguments.of("https://megaad.nz/vpn"),
            Arguments.of("https://mega.io/vpn?param=value"),
            Arguments.of("https://mega.nz/vpn/features"),
            // New megaad subdomain variations
            Arguments.of("https://vpn.megaad.co.nz"),
            Arguments.of("https://vpn.megaad.io"),
            Arguments.of("https://vpn.megaad.app"),
            // New megaad path-based variations
            Arguments.of("https://megaad.co.nz/vpn"),
            Arguments.of("https://megaad.io/vpn"),
            Arguments.of("https://megaad.app/vpn"),
            Arguments.of("https://megaad.co.nz/vpn?param=value")
        )

        @JvmStatic
        fun pwmUrlsProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("https://pwm.mega.nz"),
            Arguments.of("https://pwm.mega.nz/"),
            Arguments.of("https://pwm.mega.nz/path"),
            Arguments.of("https://pwm.mega.nz?param=value"),
            Arguments.of("https://pwm.mega.app"),
            Arguments.of("https://pwm.mega.app/"),
            Arguments.of("https://pwm.mega.app/path"),
            Arguments.of("https://pwm.mega.app?param=value"),
            Arguments.of("https://pwm.mega.co.nz"),
            Arguments.of("https://pwm.mega.io"),
            Arguments.of("https://pwm.megaad.nz"),
            Arguments.of("https://pwm.mega.nz/vault"),
            Arguments.of("https://pwm.mega.nz/settings?tab=security"),
            Arguments.of("https://pwm.mega.io/password-generator"),
            // Path-based PWM URLs
            Arguments.of("https://mega.io/pass"),
            Arguments.of("https://mega.nz/pass"),
            Arguments.of("https://mega.co.nz/pass"),
            Arguments.of("https://mega.app/pass"),
            Arguments.of("https://megaad.nz/pass"),
            Arguments.of("https://mega.io/pass?param=value"),
            Arguments.of("https://mega.nz/pass/features"),
            // New megaad subdomain variations
            Arguments.of("https://pwm.megaad.co.nz"),
            Arguments.of("https://pwm.megaad.io"),
            Arguments.of("https://pwm.megaad.app"),
            // New megaad path-based variations
            Arguments.of("https://megaad.co.nz/pass"),
            Arguments.of("https://megaad.io/pass"),
            Arguments.of("https://megaad.app/pass"),
            Arguments.of("https://megaad.co.nz/pass?param=value")
        )

        @JvmStatic
        fun transferItUrlsProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("https://mega.co.nz/transfer-it"),
            Arguments.of("https://mega.co.nz/transfer-it?mct=btrand"),
            Arguments.of("https://mega.nz/transfer-it?param=value"),
            Arguments.of("https://mega.io/transfer-it?"),
            Arguments.of("https://megaad.nz/transfer-it?mct=btrand"),
            Arguments.of("https://mega.app/transfer-it?param=value"),
            Arguments.of("https://mega.nz/transfer-it?mct=btrand&size=large"),
            Arguments.of("https://mega.app/transfer-it?id=12345&mode=upload"),
            // New megaad domain variations
            Arguments.of("https://megaad.co.nz/transfer-it"),
            Arguments.of("https://megaad.io/transfer-it?mct=btrand"),
            Arguments.of("https://megaad.app/transfer-it?param=value"),
            Arguments.of("https://megaad.co.nz/transfer-it?feature=upload")
        )

        @JvmStatic
        fun invalidVpnUrlsProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("https://mega.nz"),
            Arguments.of("https://pwm.mega.nz"),
            Arguments.of("https://vpn.google.com"),
            Arguments.of("http://vpn.mega.nz") // http instead of https
        )

        @JvmStatic
        fun invalidPwmUrlsProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("https://mega.nz"),
            Arguments.of("https://vpn.mega.nz"),
            Arguments.of("https://pwm.google.com"),
            Arguments.of("http://pwm.mega.nz") // http instead of https
        )

        @JvmStatic
        fun invalidTransferItUrlsProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("https://mega.nz"),
            Arguments.of("https://mega.nz/other-page?param=value"),
            Arguments.of("https://google.com/transfer-it?param=value"),
            Arguments.of("http://mega.nz/transfer-it?param=value") // http instead of https
        )
    }

    private val context: Context = mock()
    private val navigationHandler: NavigationHandler = mock()

    @ParameterizedTest
    @MethodSource("vpnUrlsProvider")
    fun `test that VPN URL matching for valid URLs`(url: String) {
        assertTrue(BannerClickHandler.matchesVpnUrl(url))
    }

    @ParameterizedTest
    @MethodSource("invalidVpnUrlsProvider")
    fun `test that VPN URL non-matching cases`(url: String) {
        assertFalse(BannerClickHandler.matchesVpnUrl(url))
    }

    @ParameterizedTest
    @MethodSource("pwmUrlsProvider")
    fun `test that PWM URL matching for valid URLs`(url: String) {
        assertTrue(BannerClickHandler.matchesPwmUrl(url))
    }

    @ParameterizedTest
    @MethodSource("invalidPwmUrlsProvider")
    fun `test that PWM URL non-matching cases`(url: String) {
        assertFalse(BannerClickHandler.matchesPwmUrl(url))
    }

    @ParameterizedTest
    @MethodSource("transferItUrlsProvider")
    fun `test Transfer-it URL matching for valid URLs`(url: String) {
        assertTrue(BannerClickHandler.matchesTransferItUrl(url))
    }

    @ParameterizedTest
    @MethodSource("invalidTransferItUrlsProvider")
    fun `test that Transfer-it URL non-matching cases`(url: String) {
        assertFalse(BannerClickHandler.matchesTransferItUrl(url))
    }

    @Test
    fun `test that handleBannerClick for VPN URL`() {
        val vpnUrl = "https://vpn.mega.nz"

        BannerClickHandler.handleBannerClick(context, navigationHandler, vpnUrl)

        assertTrue(analyticsExtension.events.contains(VpnSmartBannerItemSelectedEvent))
        verify(context).startActivity(any<Intent>())
    }

    @Test
    fun `test that handleBannerClick for PWM URL`() {
        val pwmUrl = "https://pwm.mega.nz"

        BannerClickHandler.handleBannerClick(context, navigationHandler, pwmUrl)

        assertTrue(analyticsExtension.events.contains(PwmSmartBannerItemSelectedEvent))
        verify(context).startActivity(any<Intent>())
    }

    @Test
    fun `test that handleBannerClick for Transfer-it URL`() {
        val transferItUrl = "https://mega.io/transfer-it?mct=btrand"

        BannerClickHandler.handleBannerClick(context, navigationHandler, transferItUrl)

        assertTrue(analyticsExtension.events.contains(TransferItSmartBannerItemSelectedEvent))
        verify(navigationHandler).navigate(WebSiteNavKey(transferItUrl))
    }

    @Test
    fun `test that handleBannerClick for other MEGA URL falls back to WebSite navigation`() {
        val otherUrl = "https://mega.nz/some-other-page"

        BannerClickHandler.handleBannerClick(context, navigationHandler, otherUrl)

        verify(navigationHandler).navigate(WebSiteNavKey(otherUrl))
        assertTrue(analyticsExtension.events.isEmpty())
    }

    @Test
    fun `test that VPN app opening fallback to Play Store when VPN app not installed`() {
        val vpnUrl = "https://vpn.mega.nz"
        var callCount = 0

        doAnswer {
            callCount++
            when (callCount) {
                1 -> throw RuntimeException("App not found")
                2 -> throw RuntimeException("Market not found")
                else -> Unit // Success on third call
            }
        }.`when`(context).startActivity(any<Intent>())

        BannerClickHandler.handleBannerClick(context, navigationHandler, vpnUrl)

        assertTrue(analyticsExtension.events.contains(VpnSmartBannerItemSelectedEvent))

        // Verify all three attempts were made: app -> market -> web store
        verify(context, times(3)).startActivity(any<Intent>())
    }

    @Test
    fun `test that PWM app opening fallback to Play Store when PWM app not installed`() {
        val pwmUrl = "https://pwm.mega.nz"
        var callCount = 0

        doAnswer {
            callCount++
            when (callCount) {
                1 -> throw RuntimeException("App not found")
                2 -> throw RuntimeException("Market not found")
                else -> Unit // Success on third call
            }
        }.`when`(context).startActivity(any<Intent>())

        BannerClickHandler.handleBannerClick(context, navigationHandler, pwmUrl)


        assertTrue(analyticsExtension.events.contains(PwmSmartBannerItemSelectedEvent))

        // Verify all three attempts were made: app -> market -> web store
        verify(context, times(3)).startActivity(any<Intent>())
    }

    @Test
    fun `test that specific megaad domain variations are supported`() {
        // VPN megaad domains - subdomain format
        assertTrue(BannerClickHandler.matchesVpnUrl("https://vpn.megaad.co.nz"))
        assertTrue(BannerClickHandler.matchesVpnUrl("https://vpn.megaad.io"))
        assertTrue(BannerClickHandler.matchesVpnUrl("https://vpn.megaad.app"))
        assertTrue(BannerClickHandler.matchesVpnUrl("https://vpn.megaad.nz"))

        // VPN megaad domains - path format
        assertTrue(BannerClickHandler.matchesVpnUrl("https://megaad.co.nz/vpn"))
        assertTrue(BannerClickHandler.matchesVpnUrl("https://megaad.io/vpn"))
        assertTrue(BannerClickHandler.matchesVpnUrl("https://megaad.app/vpn"))
        assertTrue(BannerClickHandler.matchesVpnUrl("https://megaad.nz/vpn"))

        // PWM megaad domains - subdomain format
        assertTrue(BannerClickHandler.matchesPwmUrl("https://pwm.megaad.co.nz"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://pwm.megaad.io"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://pwm.megaad.app"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://pwm.megaad.nz"))

        // PWM megaad domains - path format
        assertTrue(BannerClickHandler.matchesPwmUrl("https://megaad.co.nz/pass"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://megaad.io/pass"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://megaad.app/pass"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://megaad.nz/pass"))

        // Transfer-it megaad domains
        assertTrue(BannerClickHandler.matchesTransferItUrl("https://megaad.co.nz/transfer-it"))
        assertTrue(BannerClickHandler.matchesTransferItUrl("https://megaad.io/transfer-it"))
        assertTrue(BannerClickHandler.matchesTransferItUrl("https://megaad.app/transfer-it"))
        assertTrue(BannerClickHandler.matchesTransferItUrl("https://megaad.nz/transfer-it"))
    }

    @Test
    fun `test that path urls are supported`() {
        assertTrue(BannerClickHandler.matchesVpnUrl("https://mega.io/vpn"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://mega.io/pass"))
        assertTrue(BannerClickHandler.matchesTransferItUrl("https://mega.io/transfer-it"))
    }

    @Test
    fun `test that URL matching edge cases with different paths and parameters`() {
        // VPN URLs with various paths and parameters
        assertTrue(BannerClickHandler.matchesVpnUrl("https://vpn.mega.nz/login"))
        assertTrue(BannerClickHandler.matchesVpnUrl("https://vpn.mega.nz/dashboard?user=test"))
        assertTrue(BannerClickHandler.matchesVpnUrl("https://vpn.mega.co.nz/path/to/page"))

        // PWM URLs with various paths and parameters
        assertTrue(BannerClickHandler.matchesPwmUrl("https://pwm.mega.nz/vault"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://pwm.mega.nz/settings?tab=security"))
        assertTrue(BannerClickHandler.matchesPwmUrl("https://pwm.mega.io/password-generator"))

        // Transfer-it URLs with different parameters
        assertTrue(BannerClickHandler.matchesTransferItUrl("https://mega.nz/transfer-it?mct=btrand&size=large"))
        assertTrue(BannerClickHandler.matchesTransferItUrl("https://mega.app/transfer-it?id=12345&mode=upload"))
    }
}
