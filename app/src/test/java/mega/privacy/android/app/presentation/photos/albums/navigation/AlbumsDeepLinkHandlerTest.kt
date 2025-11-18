package mega.privacy.android.app.presentation.photos.albums.navigation


import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.destination.LegacyAlbumImportNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumsDeepLinkHandlerTest {

    private lateinit var underTest: AlbumsDeepLinkHandler


    @BeforeAll
    fun setup() {
        underTest = AlbumsDeepLinkHandler()
    }

    @Test
    fun `test that correct nav key is returned when uri matches ALBUM_LINK pattern type`() =
        runTest {
            val uriString = "mega://albumLink"
            val expected = LegacyAlbumImportNavKey(link = uriString)
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.ALBUM_LINK)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match ALBUM_LINK pattern type`() =
        runTest {
            val uriString = "mega://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK)

            assertThat(actual).isNull()
        }
}