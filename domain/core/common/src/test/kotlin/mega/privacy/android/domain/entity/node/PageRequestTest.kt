package mega.privacy.android.domain.entity.node

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PageRequestTest {

    @Test
    fun `test that a request with a non-negative offset and a positive limit is accepted`() {
        val actual = PageRequest(offset = 0, limit = 1)

        assertThat(actual.offset).isEqualTo(0)
        assertThat(actual.limit).isEqualTo(1)
    }

    @Test
    fun `test that a negative offset is rejected`() {
        assertThrows<IllegalArgumentException> { PageRequest(offset = -1, limit = 1) }
    }

    @Test
    fun `test that a zero limit is rejected`() {
        assertThrows<IllegalArgumentException> { PageRequest(offset = 0, limit = 0) }
    }
}
