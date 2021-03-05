package mega.privacy.android.app.interfaces

import android.content.Intent

interface ActivityLauncher {
    fun launchActivity(intent: Intent)

    fun launchActivityForResult(intent: Intent, requestCode: Int)

    companion object {
        val IDLE = object : ActivityLauncher {
            override fun launchActivity(intent: Intent) {}

            override fun launchActivityForResult(intent: Intent, requestCode: Int) {}
        }
    }
}
