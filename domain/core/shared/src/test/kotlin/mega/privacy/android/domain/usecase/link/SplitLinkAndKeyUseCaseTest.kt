package mega.privacy.android.domain.usecase.link

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SplitLinkAndKeyUseCaseTest {

    private val underTest = SplitLinkAndKeyUseCase()

    @Test
    fun `test that a new-format link is split into link without key and key`() {
        val result = underTest("https://mega.nz/file/abc123#decryptionKey")

        assertThat(result.linkWithoutKey).isEqualTo("https://mega.nz/file/abc123")
        assertThat(result.key).isEqualTo("decryptionKey")
    }

    @Test
    fun `test that an old-format file link is split on the exclamation mark`() {
        val result = underTest("https://mega.nz/#!abc123!decryptionKey")

        assertThat(result.linkWithoutKey).isEqualTo("https://mega.nz/#!abc123")
        assertThat(result.key).isEqualTo("decryptionKey")
    }

    @Test
    fun `test that an old-format folder link is split on the exclamation mark`() {
        val result = underTest("https://mega.nz/#F!folderId!folderKey")

        assertThat(result.linkWithoutKey).isEqualTo("https://mega.nz/#F!folderId")
        assertThat(result.key).isEqualTo("folderKey")
    }

    @Test
    fun `test that a link with no key returns null parts`() {
        val result = underTest("https://mega.nz/file/abc123")

        assertThat(result.linkWithoutKey).isNull()
        assertThat(result.key).isNull()
    }
}
