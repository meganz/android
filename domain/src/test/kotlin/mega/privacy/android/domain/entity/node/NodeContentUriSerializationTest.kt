package mega.privacy.android.domain.entity.node

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import mega.privacy.android.domain.entity.node.serialisation.nodeSerialisationModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

/**
 * Tests that [NodeContentUri] can be serialized and deserialized correctly.
 */
class NodeContentUriSerializationTest {

    private val json = Json

    @ParameterizedTest(name = "when shouldStopHttpSever is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that RemoteContentUri round-trips with default Json`(shouldStopHttpSever: Boolean) {
        val original = NodeContentUri.RemoteContentUri(
            url = "https://example.com/file.mp4",
            shouldStopHttpSever = shouldStopHttpSever
        )
        val encoded = json.encodeToString(serializer<NodeContentUri>(), original)
        val decoded: NodeContentUri = json.decodeFromString(encoded)

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded).isInstanceOf(NodeContentUri.RemoteContentUri::class.java)
    }

    @Test
    fun `test that LocalContentUri round-trips with default Json`(@TempDir tempDir: File) {
        val file = File(tempDir, "test.txt")
        file.createNewFile()
        val original = NodeContentUri.LocalContentUri(file)

        val encoded = json.encodeToString(serializer<NodeContentUri>(), original)
        val decoded = json.decodeFromString(serializer<NodeContentUri>(), encoded)

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded).isInstanceOf(NodeContentUri.LocalContentUri::class.java)
    }
}
