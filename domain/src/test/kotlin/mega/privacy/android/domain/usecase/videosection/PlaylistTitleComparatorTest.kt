package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaylistTitleComparatorTest {

    @Test
    fun `test that a trailing counter is ordered by value`() {
        val titles = listOf(
            "New playlist (10)",
            "New playlist",
            "New playlist (2)",
            "New playlist (1)",
            "New playlist (11)",
        )

        assertThat(titles.sortedWith(playlistTitleComparator)).containsExactly(
            "New playlist",
            "New playlist (1)",
            "New playlist (2)",
            "New playlist (10)",
            "New playlist (11)",
        ).inOrder()
    }

    @Test
    fun `test that digits are ordered by value at any position`() {
        val titles = listOf("Trip 10 videos", "Trip 9 videos")

        assertThat(titles.sortedWith(playlistTitleComparator))
            .containsExactly("Trip 9 videos", "Trip 10 videos").inOrder()
    }

    @Test
    fun `test that titles are ordered ignoring case`() {
        val titles = listOf("banana", "Apple", "cherry")

        assertThat(titles.sortedWith(playlistTitleComparator))
            .containsExactly("Apple", "banana", "cherry").inOrder()
    }

    @Test
    fun `test that a counter too large for Long is ordered by value`() {
        val titles = listOf(
            "Playlist (100000000000000000000)",
            "Playlist (99999999999999999999)",
        )

        assertThat(titles.sortedWith(playlistTitleComparator)).containsExactly(
            "Playlist (99999999999999999999)",
            "Playlist (100000000000000000000)",
        ).inOrder()
    }

    @Test
    fun `test that titles differing only in case are ordered deterministically`() {
        assertThat(listOf("playlist", "Playlist").sortedWith(playlistTitleComparator))
            .isEqualTo(listOf("Playlist", "playlist").sortedWith(playlistTitleComparator))
    }

    @Test
    fun `test that titles differing only in leading zeros are ordered deterministically`() {
        assertThat(listOf("Playlist (01)", "Playlist (1)").sortedWith(playlistTitleComparator))
            .isEqualTo(listOf("Playlist (1)", "Playlist (01)").sortedWith(playlistTitleComparator))
    }

    @Test
    fun `test that an empty title is ordered first`() {
        val titles = listOf("Playlist", "", "1")

        assertThat(titles.sortedWith(playlistTitleComparator))
            .containsExactly("", "1", "Playlist").inOrder()
    }
}
