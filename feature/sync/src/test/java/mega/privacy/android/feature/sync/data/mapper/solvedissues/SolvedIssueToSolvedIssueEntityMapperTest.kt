package mega.privacy.android.feature.sync.data.mapper.solvedissues

import com.google.common.truth.Truth
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.feature.sync.data.mapper.solvedissue.SolvedIssueToSolvedIssueEntityMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SolvedIssueToSolvedIssueEntityMapperTest {

    private val gson = Gson()
    private val listToStringWithDelimitersMapper = ListToStringWithDelimitersMapper(gson)
    private val encryptData: EncryptData = mock()

    private lateinit var underTest: SolvedIssueToSolvedIssueEntityMapper

    @BeforeEach
    fun setUp() {
        underTest = SolvedIssueToSolvedIssueEntityMapper(
            listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
            encryptData = encryptData
        )
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val nodeIds = listOf(NodeId(1L))
        val paths = listOf("some path")
        val resolution = "Some resolution"

        val nodeIdsJson = gson.toJson(nodeIds)
        val pathJson = gson.toJson(paths)

        val encryptedNodeId = "Encrypted Node IDs"
        val encryptedPaths = "Encrypted paths"
        val encryptedResolution = "Encrypted resolution"

        whenever(encryptData(nodeIdsJson)).thenReturn(encryptedNodeId)
        whenever(encryptData(pathJson)).thenReturn(encryptedPaths)
        whenever(encryptData(resolution)).thenReturn(encryptedResolution)

        val expected = SyncSolvedIssueEntity(
            nodeIds = encryptedNodeId,
            localPaths = encryptedPaths,
            resolutionExplanation = encryptedResolution
        )

        val solvedIssues = SolvedIssue(
            nodeIds = nodeIds,
            localPaths = paths,
            resolutionExplanation = resolution
        )

        val actual = underTest(solvedIssues)

        Truth.assertThat(actual).isEqualTo(expected)
    }
}