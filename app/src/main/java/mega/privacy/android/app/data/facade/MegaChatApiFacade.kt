package mega.privacy.android.app.data.facade

import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaChatRequestListenerInterface
import javax.inject.Inject


/**
 * Mega chat api facade implementation of the [MegaChatApiGateway]
 *
 */
class MegaChatApiFacade @Inject constructor(
    private val chatApi: MegaChatApiAndroid
) : MegaChatApiGateway {

    override val initState: Int
        get() = chatApi.initState

    override fun init(session: String): Int =
        chatApi.init(session)

    override fun logout() = chatApi.logout()

    override fun setLogger(logger: MegaChatLoggerInterface) =
        MegaChatApiAndroid.setLoggerObject(logger)

    override fun setLogLevel(logLevel: Int) = MegaChatApiAndroid.setLogLevel(logLevel)

    override fun addChatRequestListener(listener: MegaChatRequestListenerInterface) =
        chatApi.addChatRequestListener(listener)

    override fun removeChatRequestListener(listener: MegaChatRequestListenerInterface) =
        chatApi.removeChatRequestListener(listener)

    override fun pushReceived(beep: Boolean, listener: MegaChatRequestListenerInterface) =
        chatApi.pushReceived(beep, listener)

    override fun retryPendingConnections(disconnect: Boolean) =
        chatApi.retryPendingConnections(disconnect, null)
}