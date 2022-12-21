package mega.privacy.android.app.presentation.photos.albums.photosselection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import javax.inject.Inject

@AndroidEntryPoint
class AlbumPhotosSelectionActivity : AppCompatActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                // TODO...
            }
        }
    }

    companion object {
        private const val ALBUM_ID: String = "album_id"

        const val MESSAGE: String = "message"

        fun create(context: Context, albumId: AlbumId): Intent {
            return Intent(context, AlbumPhotosSelectionActivity::class.java).apply {
                putExtra(ALBUM_ID, albumId.id)
            }
        }
    }
}
