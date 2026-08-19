package mega.privacy.android.feature.mediaplayer.data

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AudioQueueItemFactoryTest {

    private lateinit var underTest: AudioQueueItemFactory
    private lateinit var mediaHandleStore: MediaHandleStore

    @BeforeEach
    fun setUp() {
        mediaHandleStore = MediaHandleStore()
        underTest = AudioQueueItemFactory(mediaHandleStore)
    }

    private fun metadata(
        title: String? = null,
        displayTitle: String? = null,
        artist: String? = null,
    ): MediaMetadata = MediaMetadata.Builder()
        .setTitle(title)
        .setDisplayTitle(displayTitle)
        .setArtist(artist)
        .build()

    private fun mediaItem(
        mediaId: String,
        title: String? = null,
        displayTitle: String? = null,
        artist: String? = null,
    ): MediaItem = MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(metadata(title, displayTitle, artist))
        .build()

    @Test
    fun `test that resolveTitle returns tag title when present`() {
        val result = underTest.resolveTitle(metadata(title = "Song", displayTitle = "file.mp3"))

        assertThat(result).isEqualTo("Song")
    }

    @Test
    fun `test that resolveTitle falls back to display title when tag title is absent`() {
        val result = underTest.resolveTitle(metadata(displayTitle = "file.mp3"))

        assertThat(result).isEqualTo("file.mp3")
    }

    @Test
    fun `test that resolveTitle returns null when no title is present`() {
        assertThat(underTest.resolveTitle(metadata())).isNull()
    }

    @Test
    fun `test that buildQueueItems maps media id title artist and handle`() {
        mediaHandleStore.register("id-1", 42L)

        val result = underTest.buildQueueItems(
            listOf(mediaItem(mediaId = "id-1", title = "Song", artist = "Artist"))
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].mediaId).isEqualTo("id-1")
        assertThat(result[0].title).isEqualTo("Song")
        assertThat(result[0].artist).isEqualTo("Artist")
        assertThat(result[0].handle).isEqualTo(42L)
    }

    @Test
    fun `test that buildQueueItems falls back to display title when item has no tag title`() {
        val result = underTest.buildQueueItems(
            listOf(mediaItem(mediaId = "id-1", displayTitle = "file.mp3"))
        )

        assertThat(result[0].title).isEqualTo("file.mp3")
        assertThat(result[0].artist).isNull()
    }

    @Test
    fun `test that buildQueueItems uses cached tag metadata when item metadata has none`() {
        underTest.cacheParsedMetadata("id-1", metadata(title = "Real song", artist = "Real artist"))

        val result = underTest.buildQueueItems(
            listOf(mediaItem(mediaId = "id-1", displayTitle = "file.mp3"))
        )

        assertThat(result[0].title).isEqualTo("Real song")
        assertThat(result[0].artist).isEqualTo("Real artist")
    }

    @Test
    fun `test that cacheParsedMetadata returns false when metadata has no tag title`() {
        val cached = underTest.cacheParsedMetadata(
            "id-1",
            metadata(displayTitle = "file.mp3", artist = "Artist"),
        )

        assertThat(cached).isFalse()

        val result = underTest.buildQueueItems(
            listOf(mediaItem(mediaId = "id-1", displayTitle = "file.mp3"))
        )
        assertThat(result[0].title).isEqualTo("file.mp3")
    }

    @Test
    fun `test that pruneCache drops entries not in the active ids`() {
        underTest.cacheParsedMetadata("keep", metadata(title = "Kept song"))
        underTest.cacheParsedMetadata("drop", metadata(title = "Dropped song"))

        underTest.pruneCache(setOf("keep"))

        val result = underTest.buildQueueItems(
            listOf(
                mediaItem(mediaId = "keep", displayTitle = "keep.mp3"),
                mediaItem(mediaId = "drop", displayTitle = "drop.mp3"),
            )
        )
        assertThat(result[0].title).isEqualTo("Kept song")
        assertThat(result[1].title).isEqualTo("drop.mp3")
    }

    @Test
    fun `test that clearCache removes all cached metadata`() {
        underTest.cacheParsedMetadata("id-1", metadata(title = "Song"))

        underTest.clearCache()

        val result = underTest.buildQueueItems(
            listOf(mediaItem(mediaId = "id-1", displayTitle = "file.mp3"))
        )
        assertThat(result[0].title).isEqualTo("file.mp3")
    }
}
