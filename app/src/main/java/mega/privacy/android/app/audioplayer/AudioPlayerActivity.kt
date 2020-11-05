package mega.privacy.android.app.audioplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util

@AndroidEntryPoint
class AudioPlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.extras == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_audio_player)

        Util.changeStatusBarColor(this, window, R.color.black_20_opacity)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // TODO: handle notification click event
    }
}
