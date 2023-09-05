package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.data.model.node.OfflineInformation.Companion.FILE
import mega.privacy.android.data.model.node.OfflineInformation.Companion.FOLDER
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfflineInformationMapperTest {

    private val expectedPath = "path"
    private val expectedName = "name"
    private val expectedHandle = "handle"
    private val expectedIncomingHandle = "incomingHandle"
    private val parentId = 1

    private lateinit var underTest: OfflineInformationMapper

    @BeforeAll
    fun setUp() {
        underTest = OfflineInformationMapper()
    }

    @TestFactory
    fun `test mapping`() =
        listOf(true, false).flatMap { isFolderNode ->
            listOf(
                OfflineInformation.INCOMING to IncomingShareOfflineNodeInformation(
                    path = expectedPath,
                    name = expectedName,
                    handle = expectedHandle,
                    incomingHandle = expectedIncomingHandle,
                    isFolder = isFolderNode,
                ),
                OfflineInformation.BACKUPS to BackupsOfflineNodeInformation(
                    path = expectedPath,
                    name = expectedName,
                    handle = expectedHandle,
                    isFolder = isFolderNode,
                ),
                OfflineInformation.OTHER to OtherOfflineNodeInformation(
                    path = expectedPath,
                    name = expectedName,
                    handle = expectedHandle,
                    isFolder = isFolderNode,
                )
            ).map { Triple(it.first, it.second, isFolderNode) }
        }.map { (expected, input, isFolderNode) ->
            DynamicTest.dynamicTest("test that ${input::class.java.simpleName} is mapped to correct values. Folder: $isFolderNode") {
                assertThat(underTest(input, parentId)).isEqualTo(
                    getExpected(
                        expected,
                        isFolderNode
                    )
                )
            }
        }

    private fun getExpected(origin: Int, isFolderNode: Boolean) = OfflineInformation(
        id = -1,
        handle = "handle",
        path = expectedPath,
        name = expectedName,
        parentId = parentId,
        type = if (isFolderNode) FOLDER else FILE,
        origin = origin,
        handleIncoming = if (origin == OfflineInformation.INCOMING) expectedIncomingHandle else ""
    )
}