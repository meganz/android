package mega.privacy.android.feature.sync.data.mapper.solvedissues

import com.google.common.truth.Truth
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.feature.sync.data.mapper.solvedissue.SolvedIssueEntityToSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SolvedIssueEntityToSolvedIssueMapperTest {
    private val gson = Gson()
    private val listToStringWithDelimitersMapper = ListToStringWithDelimitersMapper(gson)

    private lateinit var underTest: SolvedIssueEntityToSolvedIssueMapper

    @BeforeEach
    fun setUp() {
        underTest = SolvedIssueEntityToSolvedIssueMapper(
            listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
        )
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val syncId = 19203L
        val resolution = "Some resolution"
        val nodeIdsJson = "[{\"longValue\":1}]"
        val pathJson = "[\"some path\"]"

        val expected = SolvedIssue(
            syncId = syncId,
            nodeIds = listOf(NodeId(1L)),
            localPaths = listOf("some path"),
            resolutionExplanation = resolution
        )

        val solvedIssuesEntity = SyncSolvedIssueEntity(
            syncId = syncId,
            nodeIds = nodeIdsJson,
            localPaths = pathJson,
            resolutionExplanation = resolution
        )

        val actual = underTest.invoke(solvedIssuesEntity)

        Truth.assertThat(actual).isEqualTo(expected)
    }

}
