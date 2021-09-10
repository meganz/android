package mega.privacy.android.app.objects

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasscodeManagement @Inject constructor() {

    var lastPause: Long = 0
    var showPasscodeScreen: Boolean = true

    fun resetDefaults() {
        lastPause = 0
        showPasscodeScreen = true
    }
}