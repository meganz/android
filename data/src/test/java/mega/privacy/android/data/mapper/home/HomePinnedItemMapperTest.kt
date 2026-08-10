package mega.privacy.android.data.mapper.home

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.database.entity.HomePinnedItemEntity
import mega.privacy.android.domain.entity.home.PinnedHomeItem
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HomePinnedItemMapperTest {
    private lateinit var underTest: HomePinnedItemMapper

    @BeforeEach
    fun setUp() {
        underTest = HomePinnedItemMapper()
    }

    @Test
    fun `test that entity is mapped to domain model`() {
        val entity = HomePinnedItemEntity(
            nodeHandle = NODE_HANDLE,
            nodeName = NODE_NAME,
            isFolder = true,
            pinnedAt = PINNED_AT,
        )

        val result = underTest(entity)

        assertThat(result).isEqualTo(
            PinnedHomeItem(
                nodeId = NodeId(NODE_HANDLE),
                name = NODE_NAME,
                isFolder = true,
                pinnedAt = PINNED_AT,
            )
        )
    }

    @Test
    fun `test that domain model is mapped to entity`() {
        val domain = PinnedHomeItem(
            nodeId = NodeId(NODE_HANDLE),
            name = NODE_NAME,
            isFolder = false,
            pinnedAt = PINNED_AT,
        )

        val result = underTest(domain)

        assertThat(result).isEqualTo(
            HomePinnedItemEntity(
                nodeHandle = NODE_HANDLE,
                nodeName = NODE_NAME,
                isFolder = false,
                pinnedAt = PINNED_AT,
            )
        )
    }

    private companion object {
        const val NODE_HANDLE = 123L
        const val NODE_NAME = "Documents"
        const val PINNED_AT = 1_700_000_000_000L
    }
}
