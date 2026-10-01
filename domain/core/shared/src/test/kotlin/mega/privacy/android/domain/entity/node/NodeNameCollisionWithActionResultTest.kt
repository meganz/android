import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import org.junit.Test

class NodeNameCollisionWithActionResultTest {

    @Test
    fun `test firstNodeCollisionOrNull returns null when there is no collisions`() {
        val collisionResult =
            NodeNameCollisionsResult(emptyMap(), emptyMap(), NodeNameCollisionType.RESTORE)
        val actionResult = NodeNameCollisionWithActionResult(collisionResult)

        assertThat(actionResult.firstNodeCollisionOrNull).isNull()
    }

    @Test
    fun `test firstNodeCollisionOrNull returns first collision when collisions exist`() {
        val conflictNodes = mapOf(
            1L to NodeNameCollision.Default(
                collisionHandle = 1L,
                nodeHandle = 1L,
                name = "conflictNode",
                size = 1024L,
                childFolderCount = 0,
                childFileCount = 1,
                lastModified = 1625097600000L,
                parentHandle = 2L,
                isFile = true
            )
        )
        val collisionResult =
            NodeNameCollisionsResult(emptyMap(), conflictNodes, NodeNameCollisionType.COPY)
        val actionResult = NodeNameCollisionWithActionResult(collisionResult)
        assertThat(actionResult.firstNodeCollisionOrNull).isInstanceOf(NodeNameCollision.Default::class.java)
    }

    @Test
    fun `test firstChatNodeCollisionOrNull returns null and there are no chat collisions`() {
        val conflictNodes = mapOf(
            1L to NodeNameCollision.Default(
                collisionHandle = 1L,
                nodeHandle = 1L,
                name = "conflictNode",
                size = 1024L,
                childFolderCount = 0,
                childFileCount = 1,
                lastModified = 1625097600000L,
                parentHandle = 2L,
                isFile = true
            )
        )
        val collisionResult =
            NodeNameCollisionsResult(emptyMap(), conflictNodes, NodeNameCollisionType.RESTORE)
        val actionResult = NodeNameCollisionWithActionResult(collisionResult)

        assertThat(actionResult.firstChatNodeCollisionOrNull).isNull()
    }

    @Test
    fun `test firstChatNodeCollisionOrNull returns first collision when chat collisions exist`() {
        val conflictNodes = mapOf(
            1L to NodeNameCollision.Chat(
                collisionHandle = 1L,
                nodeHandle = 1L,
                name = "conflictChat",
                size = 2048L,
                childFolderCount = 0,
                childFileCount = 0,
                lastModified = 1625097600000L,
                parentHandle = 2L,
                isFile = false,
                chatId = 100L,
                messageId = 200L
            )
        )
        val collisionResult =
            NodeNameCollisionsResult(emptyMap(), conflictNodes, NodeNameCollisionType.MOVE)
        val actionResult = NodeNameCollisionWithActionResult(collisionResult)

        assertThat(actionResult.firstChatNodeCollisionOrNull).isInstanceOf(NodeNameCollision.Chat::class.java)
    }
}