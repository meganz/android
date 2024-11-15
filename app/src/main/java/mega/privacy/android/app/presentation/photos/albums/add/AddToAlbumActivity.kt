package mega.privacy.android.app.presentation.photos.albums.add

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

@AndroidEntryPoint
internal class AddToAlbumActivity : BaseActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val addToAlbumViewModel: AddToAlbumViewModel by viewModels()

    private val photoDownloaderViewModel: PhotoDownloaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                AddToAlbumScreen(
                    addToAlbumViewModel = addToAlbumViewModel,
                    photoDownloaderViewModel = photoDownloaderViewModel,
                    onClose = ::handleCompletion,
                )
            }
        }
    }

    private fun handleCompletion(message: String) {
        if (message.isNotBlank()) {
            val data = Intent().apply {
                putExtra("message", message)
            }
            setResult(RESULT_OK, data)
        }
        finish()
    }
}
