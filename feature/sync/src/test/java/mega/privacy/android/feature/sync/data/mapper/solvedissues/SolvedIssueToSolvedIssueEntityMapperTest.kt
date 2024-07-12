package mega.privacy.android.feature.sync.data.mapper.solvedissues

import com.google.common.truth.Truth
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.feature.sync.data.mapper.solvedissue.SolvedIssueToSolvedIssueEntityMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SolvedIssueToSolvedIssueEntityMapperTest {

    private val gson = Gson()
    private val listToStringWithDelimitersMapper = ListToStringWithDelimitersMapper(gson)

    private lateinit var underTest: SolvedIssueToSolvedIssueEntityMapper

    @BeforeEach
    fun setUp() {
        underTest = SolvedIssueToSolvedIssueEntityMapper(
            listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
        )
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val syncId = 323L
        val nodeIds = listOf(NodeId(1L))
        val paths = listOf("some path")
        val resolution = "Some resolution"

        val nodeIdsJson = gson.toJson(nodeIds)
        val pathJson = gson.toJson(paths)

        val expected = SyncSolvedIssueEntity(
            syncId = syncId,
            nodeIds = nodeIdsJson,
            localPaths = pathJson,
            resolutionExplanation = resolution
        )

        val solvedIssues = SolvedIssue(
            syncId = syncId,
            nodeIds = nodeIds,
            localPaths = paths,
            resolutionExplanation = resolution
        )

        val actual = underTest(solvedIssues)

        Truth.assertThat(actual).isEqualTo(expected)
    }
}