package mega.privacy.android.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.contract.ViewGifActivityContract
import mega.privacy.android.app.presentation.container.SharedAppContainer
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.toGifData
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.chat.giphy.view.GiphyPickerScreen
import javax.inject.Inject

/**
 * Activity for the Giphy picker
 */
@AndroidEntryPoint
class GiphyPickerActivity : FragmentActivity() {

    companion object {
        /**
         * To specify GIF data values
         */
        const val GIF_DATA = "GIF_DATA"
    }

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val pickGifLauncher = registerForActivityResult(ViewGifActivityContract()) { gifData ->
        if (gifData != null) {
            setResult(RESULT_OK, Intent().putExtra(GIF_DATA, gifData))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            SharedAppContainer(
                themeMode = themeMode,
                useLegacyStatusBarColor = false,
            ) {
                GiphyPickerScreen(
                    onGifSelected = { pickGifLauncher.launch(it.toGifData()) },
                    onBack = ::finish,
                )
            }
        }
    }
}
