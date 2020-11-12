package mega.privacy.android.app.audioplayer

import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

/**
 * This class will receive metadata from ExoPlayer.
 * */
class MetadataExtractor(
    private val trackSelector: MappingTrackSelector,
    private val callback: (String?, String?, String?) -> Unit
) : Player.EventListener {
    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {
        super.onTracksChanged(trackGroups, trackSelections)

        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            val trackSelection = trackSelections[rendererIndex] ?: continue
            for (selectionIndex in 0 until trackSelection.length()) {
                val metadata = trackSelection.getFormat(selectionIndex).metadata
                if (metadata != null) {
                    extractMetadata(metadata)
                    return
                }
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
