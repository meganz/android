package mega.privacy.android.feature.sync.data.mapper.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncTransferLocationMapperTest {

    private lateinit var underTest: SyncTransferLocationMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncTransferLocationMapper()
    }

    @Test
    fun `test that invoke formats the location from the parent folder`() {
        assertThat(underTest("$ROOT/IMG_1.jpg")).isEqualTo("Camera")
    }

    @Test
    fun `test that invoke returns the raw sub folder name of a saf path`() {
        assertThat(underTest("$ROOT/2024 trip/IMG_2.jpg")).isEqualTo("2024 trip")
    }

    @Test
    fun `test that invoke keeps a literal percent sign in a sub folder name`() {
        assertThat(underTest("$ROOT/100%25 done/a.txt")).isEqualTo("100%25 done")
    }

    @Test
    fun `test that invoke returns the deepest folder of a nested saf path`() {
        assertThat(underTest("$ROOT/a/b/c.txt")).isEqualTo("b")
    }

    @Test
    fun `test that invoke decodes a multi byte root folder name`() {
        val path = "$AUTHORITY/tree/primary%3A%E5%86%99%E7%9C%9F" +
                "/document/primary%3A%E5%86%99%E7%9C%9F/a.jpg"

        assertThat(underTest(path)).isEqualTo("写真")
    }

    @Test
    fun `test that invoke keeps a plus sign in the root folder name`() {
        val encodedPlus = "$AUTHORITY/tree/primary%3AC%2B%2B/document/primary%3AC%2B%2B/a.txt"
        val literalPlus = "$AUTHORITY/tree/primary%3AA+B/document/primary%3AA+B/a.txt"

        assertThat(underTest(encodedPlus)).isEqualTo("C++")
        assertThat(underTest(literalPlus)).isEqualTo("A+B")
    }

    @Test
    fun `test that invoke reads the root of a tree uri without a document part`() {
        assertThat(underTest("$AUTHORITY/tree/primary%3ASync/a.txt")).isEqualTo("Sync")
        assertThat(underTest("$AUTHORITY/tree/primary%3ASync/sub/a.txt")).isEqualTo("sub")
    }

    @Test
    fun `test that invoke returns the folder of a raw file path document id`() {
        val id = "raw%3A%2Fstorage%2Femulated%2F0%2FDownload"

        assertThat(underTest("$AUTHORITY/tree/$id/document/$id/a.pdf")).isEqualTo("Download")
    }

    @Test
    fun `test that invoke returns the volume when the root is a storage root`() {
        val path = "$AUTHORITY/tree/primary%3A/document/primary%3A/a.txt"

        assertThat(underTest(path)).isEqualTo("primary")
    }

    @Test
    fun `test that invoke returns the document number for a downloads provider root`() {
        val treeOnly = "$DOWNLOADS_AUTHORITY/tree/msd%3A24/a.pdf"
        val withDocument = "$DOWNLOADS_AUTHORITY/tree/msd%3A24/document/msd%3A24/a.pdf"

        assertThat(underTest(treeOnly)).isEqualTo("24")
        assertThat(underTest(withDocument)).isEqualTo("24")
    }

    @Test
    fun `test that invoke returns the volume id for a removable volume root`() {
        val treeOnly = "$AUTHORITY/tree/1A2B-3C4D%3A/a.txt"
        val withDocument = "$AUTHORITY/tree/1A2B-3C4D%3A/document/1A2B-3C4D%3A/a.txt"

        assertThat(underTest(treeOnly)).isEqualTo("1A2B-3C4D")
        assertThat(underTest(withDocument)).isEqualTo("1A2B-3C4D")
    }

    @Test
    fun `test that invoke keeps a malformed escape as written`() {
        val path = "$AUTHORITY/tree/primary%3ASync%2/document/primary%3ASync%2/a.txt"

        assertThat(underTest(path)).isEqualTo("Sync%2")
    }

    @Test
    fun `test that invoke returns the parent directory name of an absolute path`() {
        assertThat(underTest("/storage/emulated/0/MEGA Sync/Photos/a.jpg")).isEqualTo("Photos")
    }

    @Test
    fun `test that invoke returns an empty location for a missing path`() {
        assertThat(underTest(null)).isEmpty()
        assertThat(underTest("")).isEmpty()
    }

    @Test
    fun `test that invoke returns an empty location for a bare file name`() {
        assertThat(underTest("IMG_1.jpg")).isEmpty()
    }

    private companion object {
        const val AUTHORITY = "content://com.android.externalstorage.documents"
        const val DOWNLOADS_AUTHORITY = "content://com.android.providers.downloads.documents"
        const val ROOT =
            "$AUTHORITY/tree/primary%3ADCIM%2FCamera/document/primary%3ADCIM%2FCamera"
    }
}
