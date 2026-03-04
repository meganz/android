package mega.privacy.android.app.providers.documentprovider

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [NodeIdToDocumentIdMapper].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeIdToDocumentIdMapperTest {

    private lateinit var underTest: NodeIdToDocumentIdMapper

    private companion object {
        private const val PREFIX = "mega_cloud_drive_root"
    }

    @BeforeEach
    fun setUp() {
        underTest = NodeIdToDocumentIdMapper()
    }

    @Test
    fun `test that invoke returns prefix colon longValue for positive NodeId`() {
        assertThat(underTest(NodeId(999L), PREFIX)).isEqualTo("$PREFIX:999")
        assertThat(underTest(NodeId(1L), PREFIX)).isEqualTo("$PREFIX:1")
        assertThat(underTest(NodeId(12345L), PREFIX)).isEqualTo("$PREFIX:12345")
    }

    @Test
    fun `test that invoke returns prefix colon longValue for zero`() {
        assertThat(underTest(NodeId(0L), PREFIX)).isEqualTo("$PREFIX:0")
    }

    @Test
    fun `test that invoke returns prefix colon longValue for negative NodeId`() {
        assertThat(underTest(NodeId(-1L), PREFIX)).isEqualTo("$PREFIX:-1")
    }

    @Test
    fun `test that invoke uses given prefix`() {
        assertThat(underTest(NodeId(100L), "custom_prefix")).isEqualTo("custom_prefix:100")
        assertThat(underTest(NodeId(100L), "a")).isEqualTo("a:100")
    }
}
