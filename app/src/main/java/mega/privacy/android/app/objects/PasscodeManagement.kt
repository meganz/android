package mega.privacy.android.app.objects

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class which manages passcode lock behaviours.
 *
 * @property lastPause          Latest timestamp when an activity was paused.
 * @property showPasscodeScreen True if should show passcode lock, false otherwise.
 * @property needsOpenAgain     True if passcode lock should be opened again, false otherwise.
 */
@Singleton
class PasscodeManagement @Inject constructor() {

    var lastPause: Long = 0
    var showPasscodeScreen: Boolean = true
    var needsOpenAgain: Boolean = false

    fun resetDefaults() {
        lastPause = 0
        showPasscodeScreen = true
        needsOpenAgain = false
    }
}