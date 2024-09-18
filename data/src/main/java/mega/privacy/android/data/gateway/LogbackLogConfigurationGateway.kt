package mega.privacy.android.data.gateway

import android.content.Context
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.qualifier.LogFileDirectory
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Logback log configuration gateway implementation of [LogConfigurationGateway]
 *
 * @property context
 */
@Singleton
class LogbackLogConfigurationGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    @LogFileDirectory private val logFileDirectory: Lazy<File>,
) : LogConfigurationGateway {
    private val mutex = Mutex()

    override suspend fun resetLoggingConfiguration() = mutex.withLock {
        val loggingContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val loggers = loggingContext.copyOfListenerList
        loggingContext.reset()
        loggingContext.putProperty("LOG_DIR", logFileDirectory.get().absolutePath)
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