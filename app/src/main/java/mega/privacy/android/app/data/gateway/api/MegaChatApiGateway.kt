package mega.privacy.android.app.data.gateway.api

import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaChatRequestListenerInterface

/**
 * Mega chat api gateway
 *
 * @constructor Create empty Mega chat api gateway
 */
interface MegaChatApiGateway {

    /**
     * Set logger
     *
     * @param logger
     */
    fun setLogger(logger: MegaChatLoggerInterface)

    /**
     * Set log level
     *
     * @param logLevel
     */
    fun setLogLevel(logLevel: Int)

    /**
     * Add chat request listener
     *
     * @param listener
     */
    fun addChatRequestListener(listener: MegaChatRequestListenerInterface)

    /**
     * Remove chat request listener
     *
     * @param listener
     */
    fun removeChatRequestListener(listener: MegaChatRequestListenerInterface)
}