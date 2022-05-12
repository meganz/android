package test.mega.privacy.android.app.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.mapper.toFavouriteInfo
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.mock

class FavouriteInfoMapperTest{

    @Test
    fun `test that values returned by gateway are used`() {
        val node = mock<MegaNode>()
        val expectedHasVersion = true
        val expectedNumChildFolders = 2
        val expectedNumChildFiles = 3
        val gateway = mock<MegaApiGateway>{
            on { hasVersion(node) }.thenReturn(expectedHasVersion)
            on { getNumChildFolders(node) }.thenReturn(expectedNumChildFolders)
            on { getNumChildFiles(node) }.thenReturn(expectedNumChildFiles)
        }

        val actual = toFavouriteInfo(node, gateway)

        assertThat(actual.node).isSameInstanceAs (node)
        assertThat(actual.hasVersion).isEqualTo (expectedHasVersion)
        assertThat(actual.numChildFolders).isEqualTo (expectedNumChildFolders)
        assertThat(actual.numChildFiles).isEqualTo (expectedNumChildFiles)
    }
}