package mega.privacy.android.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import mega.privacy.android.app.activities.PasscodeActivity

class AskForDsiplayOverActivity : PasscodeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ask_for_display_over_activity_layout)
    }

    fun notNow(v: View) {
        Toast.makeText(this, R.string.ask_for_display_over_explain, Toast.LENGTH_LONG).show()
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun toSetting(v: View) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )

        startActivity(intent)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        DatabaseHandler.getDbHandler(this).dontAskForDisplayOver()
    }
}