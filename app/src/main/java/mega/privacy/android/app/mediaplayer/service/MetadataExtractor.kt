package mega.privacy.android.app.mediaplayer.service

import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame

/**
 * This class will receive metadata from ExoPlayer.
 * */
class MetadataExtractor(
    private val callback: (String?, String?, String?) -> Unit
) : Player.Listener {

    override fun onTracksChanged(tracksInfo: Tracks) {
        super.onTracksChanged(tracksInfo)


        for (trackGroupInfo in tracksInfo.groups) {
            for (i in 0 until trackGroupInfo.length) {
                val metadata = trackGroupInfo.mediaTrackGroup.getFormat(i).metadata ?: continue

                extractMetadata(metadata)
                return
            }
        }

        callback(null, null, null)
    }

    private fun extractMetadata(metadata: Metadata) {
        var title: String? = null
        var artist: String? = null
        var album: String? = null

        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i)

            if (entry is TextInformationFrame) {
                when {
                    entry.id.startsWith(ID3_TITLE_PREFIX) -> {
                        title = entry.value
                    }
                    entry.id == ID3_ALBUM -> {
                        album = entry.value
                    }
                    entry.id.startsWith(ID3_ARTIST_PREFIX) -> {
                        artist = entry.value
                    }
                }
            }
        }
        callback(title, artist, album)
    }

    companion object {
        // reference: https://en.wikipedia.org/wiki/ID3
        private const val ID3_TITLE_PREFIX = "TIT"
        private const val ID3_ALBUM = "TALB"
        private const val ID3_ARTIST_PREFIX = "TPE"
    }
}
