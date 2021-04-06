package mega.privacy.android.app.objects

import android.content.Context

data class PasscodeManagement(
    var lastLocked: Context?,
    var lastPause: Long,
    var showPasscodeScreen: Boolean
)