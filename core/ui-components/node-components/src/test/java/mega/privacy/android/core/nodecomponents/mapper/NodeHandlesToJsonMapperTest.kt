package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeHandlesToJsonMapperTest {

    private lateinit var underTest: NodeHandlesToJsonMapper

    @BeforeEach
    fun setUp() {
        underTest = NodeHandlesToJsonMapper()
    }

    @Test
    fun `test that toJson converts list of node handles to JSON correctly`() {
        val nodeHandles = listOf(123456789L, 987654321L, 555666777L)

        val expected = "[123456789,987654321,555666777]"
        val actual = underTest(nodeHandles)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that toJson handles empty list correctly`() {
        val nodeHandles = emptyList<Long>()

        val expected = "[]"
        val actual = underTest(nodeHandles)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that toJson handles null list correctly`() {
        val expected = "[]"
        val actual = underTest(nodeHandles = null)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that toJson handles single node handle correctly`() {
        val nodeHandles = listOf(123456789L)

        val expected = "[123456789]"
        val actual = underTest(nodeHandles)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that fromJson converts JSON to list of node handles correctly`() {
        val jsonString = "[123456789,987654321,555666777]"
        val expected = listOf(123456789L, 987654321L, 555666777L)

        val actual = underTest(jsonString)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that fromJson handles empty JSON array correctly`() {
        val jsonString = "[]"
        val expected = emptyList<Long>()

        val actual = underTest(jsonString)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that fromJson handles null string correctly`() {
        val expected = emptyList<Long>()

        val actual = underTest(jsonString = null)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that fromJson handles blank string correctly`() {
        val expected = emptyList<Long>()

        val actual = underTest("")

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that fromJson handles whitespace string correctly`() {
        val expected = emptyList<Long>()

        val actual = underTest("   ")

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that fromJson handles single node handle correctly`() {
        val jsonString = "[123456789]"
        val expected = listOf(123456789L)

        val actual = underTest(jsonString)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that fromJson handles invalid JSON gracefully`() {
        val invalidJson = "invalid json"
        val expected = emptyList<Long>()

        val actual = underTest(invalidJson)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that fromJson handles malformed JSON array gracefully`() {
        val malformedJson = "[123,456,"
        val expected = emptyList<Long>()

        val actual = underTest(malformedJson)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test round trip conversion works correctly`() {
        val originalHandles = listOf(123456789L, 987654321L, 555666777L, 111222333L)

        val jsonString = underTest(originalHandles)
        val convertedHandles = underTest(jsonString)

        assertThat(convertedHandles).isEqualTo(originalHandles)
    }

    @Test
    fun `test round trip conversion with empty list works correctly`() {
        val originalHandles = emptyList<Long>()

        val jsonString = underTest(originalHandles)
        val convertedHandles = underTest(jsonString)

        assertThat(convertedHandles).isEqualTo(originalHandles)
    }
} 