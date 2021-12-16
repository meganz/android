package mega.privacy.android.app.lollipop.managerSections.settings

import nz.mega.sdk.MegaRequestListenerInterface

@Deprecated("Dependencies between fragments and their host activities need to be removed.")
interface SettingsActivity : MegaRequestListenerInterface {
    fun changeAppBarElevation(canScrollVertically: Boolean)
    fun askConfirmationDeleteAccount()
    fun showConfirmationEnableLogsKarere()
    fun showConfirmationEnableLogsSDK()
    fun showSnackbar(snackbarType: Int, string: String, megachatInvalidHandle: Long)

    val is2FAEnabled: Boolean
    var openSettingsStartScreen: Boolean
    val openSettingsQR: Boolean
    val openSettingsStorage: Boolean
}