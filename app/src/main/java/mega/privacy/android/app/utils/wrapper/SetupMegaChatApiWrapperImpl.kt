package mega.privacy.android.app.utils.wrapper

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.data.gateway.global.SetupMegaChatApiWrapper
import javax.inject.Inject

class SetupMegaChatApiWrapperImpl @Inject constructor() : SetupMegaChatApiWrapper {
    override fun invoke() {
        MegaApplication.getInstance().setupMegaChatApi()
    }
}