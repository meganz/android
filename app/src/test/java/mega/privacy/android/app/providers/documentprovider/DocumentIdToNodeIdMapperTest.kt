package mega.privacy.android.app.providers.documentprovider

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [DocumentIdToNodeIdMapper].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentIdToNodeIdMapperTest {

    private lateinit var underTest: DocumentIdToNodeIdMapper

    private companion object {
        private const val PREFIX = "mega_cloud_drive_root"
    }

    @BeforeEach
    fun setUp() {
        underTest = DocumentIdToNodeIdMapper()
    }

    @Test
    fun `test that invoke returns handle when documentId has valid format`() {
        assertThat(underTest("$PREFIX:999", PREFIX)).isEqualTo(NodeId(999L))
        assertThat(underTest("$PREFIX:1", PREFIX)).isEqualTo(NodeId(1L))
        assertThat(underTest("$PREFIX:0", PREFIX)).isEqualTo(NodeId(0L))
        assertThat(underTest("$PREFIX:12345", PREFIX)).isEqualTo(NodeId(12345L))
    }

    @Test
    fun `test that invoke returns null when documentId does not start with prefix and colon`() {
        assertThat(underTest("other_prefix:999", PREFIX)).isNull()
        assertThat(underTest("mega_cloud_drive_roo:999", PREFIX)).isNull()
        assertThat(underTest("mega_cloud_drive_root", PREFIX)).isNull()
    }

    @Test
    fun `test that invoke returns null when documentId is empty`() {
        assertThat(underTest("", PREFIX)).isNull()
    }

    @Test
    fun `test that invoke returns null when suffix is not a valid long`() {
        assertThat(underTest("$PREFIX:abc", PREFIX)).isNull()
        assertThat(underTest("$PREFIX:12.34", PREFIX)).isNull()
        assertThat(underTest("$PREFIX:", PREFIX)).isNull()
    }

    @Test
    fun `test that invoke uses given prefix`() {
        assertThat(underTest("custom_prefix:100", "custom_prefix")).isEqualTo(
            NodeId(100L)
        )
        assertThat(underTest("$PREFIX:100", "custom_prefix")).isNull()
    }
}
