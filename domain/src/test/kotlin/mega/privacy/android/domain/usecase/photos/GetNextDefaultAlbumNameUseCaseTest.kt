package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetNextDefaultAlbumNameUseCaseTest {
    private lateinit var underTest: GetNextDefaultAlbumNameUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetNextDefaultAlbumNameUseCase()
    }

    @Test
    internal fun `test that the default name is returned if no items exist in the list`() {
        val expectedDefault = "default"
        val actual = underTest(expectedDefault, emptyList())

        assertThat(actual).isEqualTo(expectedDefault)
    }

    @Test
    internal fun `test that default name with one counter is returned it it exists in the list`() {
        val default = "default"
        val expected = "$default (1)"
        val actual = underTest(default, listOf(default))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that default with two counter is returned if none and 1 already exists`() {
        val default = "default"
        val expected = "$default (2)"
        val actual = underTest(default, listOf(default, "$default (1)"))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that default is returned if only values with counters exist`() {
        val default = "default"
        val actual = underTest(default, listOf("$default (1)", "$default (2)"))

        assertThat(actual).isEqualTo(default)
    }

    @Test
    internal fun `test that missing count is returned`() {
        val default = "default"
        val actual =
            underTest(default, listOf(default, "$default (1)", "$default (2)", "$default (4)"))

        assertThat(actual).isEqualTo("$default (3)")
    }

    @Test
    internal fun `test that if there is a duplicate count it does not count`() {
        val default = "default"
        val actual = underTest(
            default,
            listOf(default, default, "$default (1)", "$default (1)", "$default (2)", "$default (2)")
        )

        assertThat(actual).isEqualTo("$default (3)")

    }
}