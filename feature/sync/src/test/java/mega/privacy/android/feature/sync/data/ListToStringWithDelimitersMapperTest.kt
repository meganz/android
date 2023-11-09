package mega.privacy.android.feature.sync.data

import com.google.common.truth.Truth
import com.google.gson.Gson
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListToStringWithDelimitersMapperTest {

    private val gson = Gson()
    private val underTest = ListToStringWithDelimitersMapper(gson)

    @Test
    fun `test that mapping from list of NodeIds to JSON is correct`() {
        val nodeIds = listOf(NodeId(2), NodeId(4), NodeId(999))

        val expected = "[{\"longValue\":2},{\"longValue\":4},{\"longValue\":999}]"
        val actual = underTest(nodeIds)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapping from list of strings to JSON is correct`() {
        val stringsList = listOf("usr/Documents", "usr/Desktop", "usr/Downloads")

        val expected = "[\"usr/Documents\",\"usr/Desktop\",\"usr/Downloads\"]"
        val actual = underTest(stringsList)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapping from JSON to list of NodeIds is correct`() {
        val nodeIdsJson = "[{\"longValue\":2},{\"longValue\":4},{\"longValue\":999}]"
        val nodeIds = listOf(NodeId(2), NodeId(4), NodeId(999))

        val result = underTest<NodeId>(nodeIdsJson)

        Truth.assertThat(result).isEqualTo(nodeIds)
    }

    @Test
    fun `test that mapping from JSON to list of Strings is correct`() {
        val stringsList = listOf("usr/Documents", "usr/Desktop", "usr/Downloads")
        val stringsListJson = "[\"usr/Documents\",\"usr/Desktop\",\"usr/Downloads\"]"

        val result = underTest<String>(stringsListJson)

        Truth.assertThat(result).isEqualTo(stringsList)
    }

}