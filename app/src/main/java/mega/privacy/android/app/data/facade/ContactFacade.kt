package mega.privacy.android.app.data.facade

import android.content.Context
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.data.wrapper.ContactWrapper
import nz.mega.sdk.MegaUser
import javax.inject.Inject

class ContactFacade @Inject constructor() : ContactWrapper {
    override fun getMegaUserNameDB(megaUser: MegaUser?): String? =
        ContactUtil.getMegaUserNameDB(megaUser)

    override fun notifyFirstNameUpdate(context: Context, userHandle: Long) {
        ContactUtil.notifyFirstNameUpdate(context, userHandle)
    }

    override fun notifyLastNameUpdate(context: Context, userHandle: Long) {
        ContactUtil.notifyLastNameUpdate(context, userHandle)
    }

    override fun notifyNicknameUpdate(context: Context, userHandle: Long) {
        ContactUtil.notifyNicknameUpdate(context, userHandle)
    }
}