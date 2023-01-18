package mega.privacy.android.app.presentation.photos.albums.photosselection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import javax.inject.Inject

@AndroidEntryPoint
class AlbumPhotosSelectionActivity : AppCompatActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: AlbumPhotosSelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                AlbumPhotosSelectionScreen(
                    viewModel,
                    onBackClicked = ::finish,
                    onCompletion = ::handleCompletion,
                )
            }
        }
    }

    private fun handleCompletion(albumId: AlbumId, message: String) {
        val data = Intent().apply {
            putExtra(ALBUM_ID, albumId.id)
            putExtra(MESSAGE, message)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        const val ALBUM_ID: String = "album_id"

        const val ALBUM_FLOW: String = "album_flow"

        const val MESSAGE: String = "message"

        fun create(
            context: Context,
            albumId: AlbumId,
            albumFlow: AlbumFlow,
        ): Intent {
            return Intent(context, AlbumPhotosSelectionActivity::class.java).apply {
                putExtra(ALBUM_ID, albumId.id)
                putExtra(ALBUM_FLOW, albumFlow.ordinal)
            }
        }
    }
}
