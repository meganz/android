package mega.privacy.android.app.presentation.photos.albums

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.albums.coverselection.AlbumCoverSelectionScreen
import mega.privacy.android.app.presentation.photos.albums.decryptionkey.AlbumDecryptionKeyScreen
import mega.privacy.android.app.presentation.photos.albums.getlink.AlbumGetLinkScreen
import mega.privacy.android.app.presentation.photos.albums.getmultiplelinks.AlbumGetMultipleLinksScreen
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumFlow
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionScreen
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class AlbumScreenWrapperActivity : AppCompatActivity() {
    private enum class AlbumScreen {
        AlbumPhotosSelectionScreen,
        AlbumCoverSelectionScreen,
        AlbumGetLinkScreen,
        AlbumGetMultipleLinksScreen,
        AlbumDecryptionKeyScreen,
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val albumScreen: AlbumScreen? by lazy(LazyThreadSafetyMode.NONE) {
        AlbumScreen.valueOf(intent.getStringExtra(ALBUM_SCREEN) ?: "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                when (albumScreen) {
                    AlbumScreen.AlbumPhotosSelectionScreen -> {
                        AlbumPhotosSelectionScreen(
                            onBackClicked = ::finish,
                            onCompletion = { albumId, numCommittedPhotos ->
                                val data = Intent().apply {
                                    putExtra(ALBUM_ID, albumId.id)
                                    putExtra(NUM_PHOTOS, numCommittedPhotos)
                                }
                                setResult(RESULT_OK, data)
                                finish()
                            },
                        )
                    }
                    AlbumScreen.AlbumCoverSelectionScreen -> {
                        AlbumCoverSelectionScreen(
                            onBackClicked = ::finish,
                            onCompletion = { message ->
                                val data = Intent().apply {
                                    putExtra(MESSAGE, message)
                                }
                                setResult(RESULT_OK, data)
                                finish()
                            },
                        )
                    }
                    AlbumScreen.AlbumGetLinkScreen -> {
                        AlbumGetLinkScreen(
                            onBack = ::finish,
                            onLearnMore = {
                                val intent = createAlbumDecryptionKeyScreen(this)
                                startActivity(intent)
                            },
                            onShareLink = { link ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, link)
                                }
                                val shareIntent = Intent.createChooser(
                                    intent,
                                    getString(R.string.general_share)
                                )
                                startActivity(shareIntent)
                            },
                        )
                    }
                    AlbumScreen.AlbumGetMultipleLinksScreen -> {
                        AlbumGetMultipleLinksScreen(
                            onBack = ::finish,
                            onShareLinks = { links ->
                                val linksString = links.joinToString(System.lineSeparator())
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, linksString)
                                }
                                val shareIntent = Intent.createChooser(
                                    intent,
                                    getString(R.string.general_share)
                                )
                                startActivity(shareIntent)
                            },
                        )
                    }
                    AlbumScreen.AlbumDecryptionKeyScreen -> {
                        AlbumDecryptionKeyScreen(
                            onBack = ::finish,
                        )
                    }
                    else -> finish()
                }
            }
        }
    }

    companion object {
        private const val ALBUM_SCREEN: String = "album_screen"

        const val ALBUM_ID: String = "album_id"

        const val ALBUM_FLOW: String = "album_flow"

        const val NUM_PHOTOS: String = "num_photos"

        const val MESSAGE: String = "message"

        fun createAlbumPhotosSelectionScreen(
            context: Context,
            albumId: AlbumId,
            albumFlow: AlbumFlow,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumPhotosSelectionScreen.name)
            putExtra(ALBUM_ID, albumId.id)
            putExtra(ALBUM_FLOW, albumFlow.ordinal)
        }

        fun createAlbumCoverSelectionScreen(
            context: Context,
            albumId: AlbumId,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumCoverSelectionScreen.name)
            putExtra(ALBUM_ID, albumId.id)
        }

        fun createAlbumGetLinkScreen(
            context: Context,
            albumId: AlbumId,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumGetLinkScreen.name)
            putExtra(ALBUM_ID, albumId.id)
        }

        fun createAlbumGetMultipleLinksScreen(
            context: Context,
            albumIds: Set<AlbumId>,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumGetMultipleLinksScreen.name)
            putExtra(ALBUM_ID, albumIds.map {
                it.id
            }.toLongArray())
        }

        fun createAlbumDecryptionKeyScreen(
            context: Context,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumDecryptionKeyScreen.name)
        }
    }
}
