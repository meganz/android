package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.wrapper.StringWrapper
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodePathMapperTest {

    private lateinit var underTest: NodePathMapper

    private val stringWrapper = mock<StringWrapper>()

    private val folderPath = "/Folder A/Folder B"
    private val getRootNode = mock<() -> MegaNode?>()
    private val getRubbishBinNode = mock<() -> MegaNode?>()

    @BeforeAll
    fun setup() {
        underTest = NodePathMapper(stringWrapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(stringWrapper)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that mapper returns correctly if node is inShare`(
        rootParentIsInShare: Boolean,
    ) = runTest {
        val incomingShares = "Incoming shares"
        val nodePath = "incoming:$folderPath"
        val expected = "$incomingShares/$folderPath"
        val node = mock<MegaNode> {
            on { isInShare } doReturn true
        }
        val rootParent = mock<MegaNode> {
            on { isInShare } doReturn rootParentIsInShare
        }

        whenever(stringWrapper.getTitleIncomingSharesExplorer()) doReturn incomingShares

        assertThat(
            underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = getRootNode,
                getRubbishBinNode = getRubbishBinNode,
                nodePath = nodePath,
            )
        ).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that mapper returns correctly if root parent is inShare`(
        nodeIsInShare: Boolean,
    ) = runTest {
        val incomingShares = "Incoming shares"
        val nodePath = "incoming:$folderPath"
        val expected = "$incomingShares/$folderPath"
        val node = mock<MegaNode> {
            on { isInShare } doReturn nodeIsInShare
        }
        val rootParent = mock<MegaNode> {
            on { isInShare } doReturn true
        }

        whenever(stringWrapper.getTitleIncomingSharesExplorer()) doReturn incomingShares

        assertThat(
            underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = getRootNode,
                getRubbishBinNode = getRubbishBinNode,
                nodePath = nodePath,
            )
        ).isEqualTo(expected)
    }

    @Test
    fun `test that mapper returns correctly if root parent is root node`() = runTest {
        val cloud = "Cloud drive"
        val expected = "$cloud$folderPath"
        val node = mock<MegaNode> {
            on { isInShare } doReturn false
        }
        val rootParent = mock<MegaNode> {
            on { isInShare } doReturn false
            on { handle } doReturn 123
        }

        whenever(getRootNode()) doReturn rootParent
        whenever(stringWrapper.getCloudDriveSection()) doReturn cloud

        assertThat(
            underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = getRootNode,
                getRubbishBinNode = getRubbishBinNode,
                nodePath = folderPath,
            )
        ).isEqualTo(expected)
    }

    @Test
    fun `test that mapper returns correctly if root parent is a rubbish bin node`() = runTest {
        val rubbish = "Rubbish bin"
        val nodePath = "//bin$folderPath"
        val expected = "$rubbish$folderPath"
        val node = mock<MegaNode> {
            on { isInShare } doReturn false
        }
        val rootParent = mock<MegaNode> {
            on { isInShare } doReturn false
            on { handle } doReturn 123
        }

        whenever(getRootNode()) doReturn null
        whenever(getRubbishBinNode()) doReturn rootParent
        whenever(stringWrapper.getRubbishBinSection()) doReturn rubbish

        assertThat(
            underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = getRootNode,
                getRubbishBinNode = getRubbishBinNode,
                nodePath = nodePath,
            )
        ).isEqualTo(expected)
    }

    @Test
    fun `test that mapper returns correctly if root parent is NOT inShare, root or rubbish bin node and node is not inShare`() =
        runTest {
            val node = mock<MegaNode> {
                on { isInShare } doReturn false
            }
            val rootParent = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn 123L
            }

            whenever(getRootNode()) doReturn null
            whenever(getRubbishBinNode()) doReturn null

            assertThat(
                underTest(
                    node = node,
                    rootParent = rootParent,
                    getRootNode = getRootNode,
                    getRubbishBinNode = getRubbishBinNode,
                    nodePath = folderPath,
                )
            ).isEqualTo(folderPath)
        }
}