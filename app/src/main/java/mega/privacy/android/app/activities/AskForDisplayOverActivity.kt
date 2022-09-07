package mega.privacy.android.app.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import mega.privacy.android.app.R
import mega.privacy.android.app.di.getDbHandler

/**
 * A dialog style activity can lead to system setting page.
 * Launched by notification even when the app is killed.
 *
 * @see mega.privacy.android.app.utils.IncomingCallNotification.toSystemSettingNotification
 */
@RequiresApi(Build.VERSION_CODES.M)
class AskForDisplayOverActivity : Activity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.ask_for_display_over_activity_layout)
    }

    /**
     * Close the activity.
     */
    fun notNow() {
        // This will keep showing after the activity is destroyed, so can't use snack bar.
        Toast.makeText(this, R.string.ask_for_display_over_explain, Toast.LENGTH_LONG).show()
        finish()
    }

    /**
     * Launch system setting page by implicit intent.
     */
    fun toSetting() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )

        startActivity(intent)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        getDbHandler().dontAskForDisplayOver()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_not_now -> notNow()
            R.id.btn_allow -> toSetting()
        }
    }
}