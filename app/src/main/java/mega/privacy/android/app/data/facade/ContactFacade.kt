package mega.privacy.android.app.data.facade

import android.content.Context
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.data.wrapper.ContactWrapper
import nz.mega.sdk.MegaStringMap
import nz.mega.sdk.MegaUser
import javax.inject.Inject

class ContactFacade @Inject constructor() : ContactWrapper {
    override fun notifyFirstNameUpdate(context: Context, userHandle: Long) {
        ContactUtil.notifyFirstNameUpdate(context, userHandle)
    }

    override fun notifyLastNameUpdate(context: Context, userHandle: Long) {
        ContactUtil.notifyLastNameUpdate(context, userHandle)
    }

    override fun updateDBNickname(contacts: List<MegaUser>, context: Context, map: MegaStringMap) {
        ContactUtil.updateDBNickname(contacts, context, map)
    }
}