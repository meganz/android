package mega.privacy.android.app.utils.wrapper

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.data.facade.security.SetLogoutFlagWrapper
import javax.inject.Inject

/**
 * Set logout flag wrapper impl
 */
class SetLogoutFlagWrapperImpl @Inject constructor() : SetLogoutFlagWrapper {
    override fun invoke(isLoggingOut: Boolean) {
        MegaApplication.isLoggingOut = isLoggingOut
    }
}