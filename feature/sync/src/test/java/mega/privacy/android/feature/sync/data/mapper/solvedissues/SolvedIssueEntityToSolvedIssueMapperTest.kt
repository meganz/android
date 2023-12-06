package mega.privacy.android.feature.sync.data.mapper.solvedissues

import com.google.common.truth.Truth
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.feature.sync.data.mapper.solvedissue.SolvedIssueEntityToSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SolvedIssueEntityToSolvedIssueMapperTest {
    private val gson = Gson()
    private val listToStringWithDelimitersMapper = ListToStringWithDelimitersMapper(gson)
    private val decryptData: DecryptData = mock()

    private lateinit var underTest: SolvedIssueEntityToSolvedIssueMapper

    @BeforeEach
    fun setUp() {
        underTest = SolvedIssueEntityToSolvedIssueMapper(
            listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
            decryptData = decryptData
        )
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val resolution = "Some resolution"
        val nodeIdsJson = "[{\"longValue\":1}]"
        val pathJson = "[\"some path\"]"

        val encryptedNodeId = "Encrypted Node IDs"
        val encryptedPaths = "Encrypted paths"
        val encryptedResolution = "Encrypted resolution"

        whenever(decryptData(encryptedNodeId)).thenReturn(nodeIdsJson)
        whenever(decryptData(encryptedPaths)).thenReturn(pathJson)
        whenever(decryptData(encryptedResolution)).thenReturn(resolution)

        val expected = SolvedIssue(
            nodeIds = listOf(NodeId(1L)),
            localPaths = listOf("some path"),
            resolutionExplanation = resolution
        )

        val solvedIssuesEntity = SyncSolvedIssueEntity(
            nodeIds = encryptedNodeId,
            localPaths = encryptedPaths,
            resolutionExplanation = encryptedResolution
        )

        val actual = underTest.invoke(solvedIssuesEntity)

        Truth.assertThat(actual).isEqualTo(expected)
    }

}