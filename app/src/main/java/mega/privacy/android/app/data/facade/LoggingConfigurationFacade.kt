package mega.privacy.android.app.data.facade

import android.content.Context
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import dagger.hilt.android.qualifiers.ApplicationContext
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject


class LoggingConfigurationFacade @Inject constructor(@ApplicationContext private val context: Context) {
    fun resetLoggingConfiguration(){
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
}