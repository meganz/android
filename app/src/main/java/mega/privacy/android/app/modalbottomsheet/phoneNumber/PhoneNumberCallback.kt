package mega.privacy.android.app.modalbottomsheet.phoneNumber

import nz.mega.sdk.MegaError

interface PhoneNumberCallback {
    fun showConfirmation(isModify: Boolean)
    fun reset()
    fun onReset(error: MegaError)
    fun onUserDataUpdate(error: MegaError)
}