package mega.privacy.android.feature.photos.presentation.albums.model

import android.content.Context
import androidx.annotation.StringRes

/**
 * Album title
 *
 */
@Deprecated("Legacy implementation. Use LocalizedText instead")
sealed interface AlbumTitle {
    /**
     * Get title string
     *
     * @param context
     * @return title string
     */
    fun getTitleString(context: Context): String

    /**
     * Resource title
     *
     * @property identifier
     */
    data class ResourceTitle(@StringRes private val identifier: Int) : AlbumTitle {
        override fun getTitleString(context: Context) = context.getString(identifier)
    }

    /**
     * String title
     *
     * @property title
     */
    data class StringTitle(val title: String) : AlbumTitle {
        override fun getTitleString(context: Context) = title
    }
}