package mega.privacy.android.app.presentation.folderlink.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class FolderLinkStateTest {

    @Test
    fun `test that showContentActions is true when nodes are fetched without errors`() {
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = FolderError.NoError
        )
        assertThat(folderLinkState.showContentActions).isTrue()
    }

}