package mega.privacy.android.app.interfaces

import android.content.Intent

interface ActivityLauncher {
    fun launchActivity(intent: Intent)

    fun launchActivityForResult(intent: Intent, requestCode: Int)
}
