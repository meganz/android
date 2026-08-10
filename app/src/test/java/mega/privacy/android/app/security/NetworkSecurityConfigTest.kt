package mega.privacy.android.app.security

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

/**
 * Guards the network security configuration: cleartext HTTP must stay permitted
 * only for the SDK local streaming server hosts (127.0.0.1 / localhost) and be
 * forbidden for every other host.
 */
@RunWith(RobolectricTestRunner::class)
class NetworkSecurityConfigTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `test that the application manifest references the network security config`() {
        // networkSecurityConfigRes is a hidden ApplicationInfo field, read via reflection.
        val configRes = ApplicationInfo::class.java
            .getField("networkSecurityConfigRes")
            .getInt(context.applicationInfo)
        assertThat(configRes).isEqualTo(R.xml.network_security_config)
    }

    @Test
    fun `test that the base config forbids cleartext traffic`() {
        assertThat(parseConfig().baseConfigCleartextPermitted).isEqualTo("false")
    }

    @Test
    fun `test that cleartext traffic is permitted only for the local streaming server hosts`() {
        val config = parseConfig()
        assertThat(config.domainConfigCleartextPermitted).isEqualTo("true")
        assertThat(config.cleartextDomains).containsExactly("127.0.0.1", "localhost")
    }

    private data class ParsedConfig(
        val baseConfigCleartextPermitted: String?,
        val domainConfigCleartextPermitted: String?,
        val cleartextDomains: List<String>,
    )

    private fun parseConfig(): ParsedConfig {
        val parser = context.resources.getXml(R.xml.network_security_config)
        var baseConfigCleartext: String? = null
        var domainConfigCleartext: String? = null
        val domains = mutableListOf<String>()
        var currentTag: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (parser.name) {
                        "base-config" ->
                            baseConfigCleartext = parser.attribute("cleartextTrafficPermitted")

                        "domain-config" ->
                            domainConfigCleartext = parser.attribute("cleartextTrafficPermitted")
                    }
                }

                XmlPullParser.TEXT -> {
                    if (currentTag == "domain") {
                        parser.text?.trim()?.takeIf { it.isNotEmpty() }?.let { domains.add(it) }
                    }
                }

                XmlPullParser.END_TAG -> currentTag = null
            }
            event = parser.next()
        }
        return ParsedConfig(baseConfigCleartext, domainConfigCleartext, domains)
    }

    private fun XmlPullParser.attribute(name: String): String? =
        (0 until attributeCount).firstOrNull { getAttributeName(it) == name }
            ?.let { getAttributeValue(it) }
}
