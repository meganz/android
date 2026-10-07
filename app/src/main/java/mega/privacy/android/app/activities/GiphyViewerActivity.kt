package mega.privacy.android.app.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.GiphyPickerActivity.Companion.GIF_DATA
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.presentation.container.SharedAppContainer
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.toGiphyUri
import mega.privacy.android.app.utils.Constants.ACTION_PREVIEW_GIPHY
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.feature.chat.giphy.view.GiphyViewerScreen

/**
 * Shows a GIF full screen. When opened from the Giphy picker it also lets the user confirm the
 * GIF has to be sent, returning it as the activity result.
 */
@AndroidEntryPoint
class GiphyViewerActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isPicking = intent.action != ACTION_PREVIEW_GIPHY
        val gifData = with(intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(GIF_DATA, GifData::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(GIF_DATA)
            }
        }

        setContent {
            SharedAppContainer(
                themeMode = ThemeMode.Dark,
                useLegacyStatusBarColor = false,
            ) {
                GiphyViewerScreen(
                    imageUrl = gifData?.webpUrl?.toGiphyUri()?.toString(),
                    title = gifData?.title,
                    showSendButton = isPicking,
                    onSend = {
                        setResult(RESULT_OK, Intent().putExtra(GIF_DATA, gifData))
                        finish()
                    },
                )
            }
        }
    }
}
