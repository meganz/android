package mega.privacy.android.app.utils.wrapper

import mega.privacy.android.app.globalmanagement.ChatApiListenerCoordinator
import mega.privacy.android.data.gateway.global.SetupMegaChatApiWrapper
import javax.inject.Inject

class SetupMegaChatApiWrapperImpl @Inject constructor(
    private val chatApiListenerCoordinator: ChatApiListenerCoordinator,
) : SetupMegaChatApiWrapper {
    override fun invoke() {
        chatApiListenerCoordinator.register()
    }
}
