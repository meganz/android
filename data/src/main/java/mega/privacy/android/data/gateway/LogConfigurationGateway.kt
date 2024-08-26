package mega.privacy.android.data.gateway

import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * Log configuration gateway
 *
 */
interface LogConfigurationGateway {
    /**
     * Reset logging configuration
     *
     */
    suspend fun resetLoggingConfiguration()
}