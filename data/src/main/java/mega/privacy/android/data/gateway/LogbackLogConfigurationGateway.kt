package mega.privacy.android.data.gateway

import android.content.Context
import android.util.Xml
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import dagger.hilt.android.qualifiers.ApplicationContext
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject


/**
 * Logback log configuration gateway implementation of [LogConfigurationGateway]
 *
 * @property context
 */
class LogbackLogConfigurationGateway @Inject constructor(@ApplicationContext private val context: Context) :
    LogConfigurationGateway {

    override suspend fun resetLoggingConfiguration() {
        val loggingContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val loggers = loggingContext.copyOfListenerList
        loggingContext.reset()
        val config = JoranConfigurator()
        config.context = loggingContext

        try {
            val inputStream: InputStream = context.assets.open("logback.xml")
            config.doConfigure(inputStream)
            for (l in loggers) {
                loggingContext.addListener(l)
            }
        } catch (e: JoranException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override suspend fun getLoggingDirectoryPath(): String? {
        val loggingConfiguration = context.assets.open("logback.xml")

        val loggingDirectoryPath = loggingConfiguration.use { stream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)
            parser.nextTag()
            readConfiguration(parser)
        }
        return loggingDirectoryPath
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readConfiguration(parser: XmlPullParser): String? {

        parser.require(XmlPullParser.START_TAG, null, "configuration")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (isLogDirectoryProperty(parser)) {
                return readEntry(parser)
            } else {
                skip(parser)
            }
        }
        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun isLogDirectoryProperty(parser: XmlPullParser): Boolean {
        if (parser.name != "property") return false
        parser.require(XmlPullParser.START_TAG, null, "property")
        val propertyName = parser.getAttributeValue(null, "name")
        return propertyName.equals("LOG_DIR")
    }

    private fun readEntry(parser: XmlPullParser): String? {
        return parser.getAttributeValue(null, "value")
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}