package mega.privacy.android.data.extensions

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class UriPathExtTest {

    @Test
    fun test_that_path_is_converted_correctly_to_uriPath_with_file_scheme() {
        val original = "/file.txt"
        val expected = "file:///file.txt"
        val actual = original.toUriPath().value
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_that_file_is_converted_correctly_to_uriPath_with_file_scheme() {
        val original = File("file.txt")
        val expected = "file:///file.txt"
        val actual = original.path.toUriPath().value
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_that_uri_with_scheme_is_converted_correctly_to_uriPath_with_same_scheme() {
        val original = "content://foo"
        val actual = original.toUriPath().value
        assertThat(actual).isEqualTo(original)
    }
}