package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.data.model.node.OfflineInformation.Companion.FILE
import mega.privacy.android.data.model.node.OfflineInformation.Companion.FOLDER
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfflineNodeInformationMapperTest {

    private val expectedPath = "path"
    private val expectedName = "name"
    private val expectedHandle = "handle"
    private val expectedIncomingHandle = "incomingHandle"

    @ParameterizedTest(name = "folder node: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that incoming offline node contains correct values`(isFolderNode: Boolean) {
        val origin = OfflineInformation.INCOMING
        val input = getInput(origin, isFolderNode)

        assertThat(toOfflineNodeInformation(input)).isEqualTo(
            IncomingShareOfflineNodeInformation(
                path = expectedPath,
                name = expectedName,
                handle = expectedHandle,
                incomingHandle = expectedIncomingHandle,
                isFolder = isFolderNode,
            )
        )
    }

    @ParameterizedTest(name = "folder node: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the backups offline node is the correct type and contains correct values`(
        isFolderNode: Boolean,
    ) {
        val origin = OfflineInformation.BACKUPS
        val input = getInput(origin, isFolderNode)

        assertThat(toOfflineNodeInformation(input)).isEqualTo(
            BackupsOfflineNodeInformation(
                path = expectedPath,
                name = expectedName,
                handle = expectedHandle,
                isFolder = isFolderNode,
            )
        )
    }


    @ParameterizedTest(name = "folder node: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that a node with source OTHER returns the correct information`(isFolderNode: Boolean) {
        val origin = OfflineInformation.OTHER
        val input = getInput(origin, isFolderNode)

        assertThat(toOfflineNodeInformation(input)).isEqualTo(
            OtherOfflineNodeInformation(
                path = expectedPath,
                name = expectedName,
                handle = expectedHandle,
                isFolder = isFolderNode,
            )
        )
    }

    private fun getInput(origin: Int, isFolderNode: Boolean) = OfflineInformation(
        id = 1,
        handle = "handle",
        path = expectedPath,
        name = expectedName,
        parentId = 1,
        type = if (isFolderNode) FOLDER else FILE,
        origin = origin,
        handleIncoming = expectedIncomingHandle
    )
}