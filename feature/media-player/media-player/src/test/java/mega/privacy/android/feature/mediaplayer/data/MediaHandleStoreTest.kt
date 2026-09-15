package mega.privacy.android.feature.mediaplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaHandleStoreTest {

    private lateinit var underTest: MediaHandleStore

    @BeforeEach
    fun setUp() {
        underTest = MediaHandleStore()
    }

    @Test
    fun `test that getHandle returns the registered handle`() {
        underTest.register("id-1", 1L)

        assertThat(underTest.getHandle("id-1")).isEqualTo(1L)
    }

    @Test
    fun `test that remove drops only the given media ids`() {
        underTest.register("id-1", 1L)
        underTest.register("id-2", 2L)

        underTest.remove(listOf("id-1"))

        assertThat(underTest.getHandle("id-1")).isNull()
        assertThat(underTest.getHandle("id-2")).isEqualTo(2L)
    }

    @Test
    fun `test that retainAll keeps only the given media ids`() {
        underTest.register("id-1", 1L)
        underTest.register("id-2", 2L)
        underTest.register("id-3", 3L)

        underTest.retainAll(listOf("id-2", "id-3"))

        assertThat(underTest.getHandle("id-1")).isNull()
        assertThat(underTest.getHandle("id-2")).isEqualTo(2L)
        assertThat(underTest.getHandle("id-3")).isEqualTo(3L)
    }

    @Test
    fun `test that retainAll with an empty collection drops every mapping`() {
        underTest.register("id-1", 1L)

        underTest.retainAll(emptyList())

        assertThat(underTest.getHandle("id-1")).isNull()
    }

    @Test
    fun `test that clear drops every mapping`() {
        underTest.register("id-1", 1L)
        underTest.register("id-2", 2L)

        underTest.clear()

        assertThat(underTest.getHandle("id-1")).isNull()
        assertThat(underTest.getHandle("id-2")).isNull()
    }
}
