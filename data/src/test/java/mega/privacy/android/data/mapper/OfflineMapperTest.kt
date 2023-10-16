package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.Offline.Companion.FILE
import mega.privacy.android.domain.entity.Offline.Companion.FOLDER
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfflineMapperTest {

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
                Offline.INCOMING to IncomingShareOfflineNodeInformation(
                    path = expectedPath,
                    name = expectedName,
                    handle = expectedHandle,
                    incomingHandle = expectedIncomingHandle,
                    isFolder = isFolderNode,
                    lastModifiedTime = 0
                ),
                Offline.BACKUPS to BackupsOfflineNodeInformation(
                    path = expectedPath,
                    name = expectedName,
                    handle = expectedHandle,
                    isFolder = isFolderNode,
                    lastModifiedTime = 0
                ),
                Offline.OTHER to OtherOfflineNodeInformation(
                    path = expectedPath,
                    name = expectedName,
                    handle = expectedHandle,
                    isFolder = isFolderNode,
                    lastModifiedTime = 0
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

    private fun getExpected(origin: Int, isFolderNode: Boolean) = Offline(
        id = -1,
        handle = "handle",
        path = expectedPath,
        name = expectedName,
        parentId = parentId,
        type = if (isFolderNode) FOLDER else FILE,
        origin = origin,
        handleIncoming = if (origin == Offline.INCOMING) expectedIncomingHandle else ""
    )
}