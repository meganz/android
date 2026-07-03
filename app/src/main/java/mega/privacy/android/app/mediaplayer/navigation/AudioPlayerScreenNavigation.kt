package mega.privacy.android.app.mediaplayer.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Navigation key for the revamped audio player Compose screen.
 *
 * Only carries a [launchId] to look up the full launch payload from
 * [mega.privacy.android.app.mediaplayer.AudioPlayerLaunchSourceHolder], avoiding
 * [android.os.TransactionTooLargeException].
 */
@Serializable
@Parcelize
data class AudioPlayerScreenNavKey(val launchId: String) : NavKey, Parcelable
