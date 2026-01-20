package mega.privacy.android.feature.clouddrive.presentation.search.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SearchPlaceholderMapperTest {
    private lateinit var underTest: SearchPlaceholderMapper

    @BeforeEach
    fun setUp() {
        underTest = SearchPlaceholderMapper()
    }

    @Test
    fun `test that when nodeName is provided, returns StringRes with search_placeholder_folder and formatArgs`() {
        val nodeName = "My Folder"
        val result = underTest(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            nodeName = nodeName,
        )

        assertThat(result).isInstanceOf(LocalizedText.StringRes::class.java)
        val stringRes = result as LocalizedText.StringRes
        assertThat(stringRes.resId).isEqualTo(sharedR.string.search_placeholder_folder)
        assertThat(stringRes.formatArgs).containsExactly(nodeName)
    }

    @Test
    fun `test that when nodeName is empty string, returns default placeholder`() {
        val result = underTest(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            nodeName = "",
        )

        assertThat(result).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result as LocalizedText.StringRes).resId)
            .isEqualTo(sharedR.string.search_placeholder_cloud_drive)
    }

    @Test
    fun `test that when nodeName is null, returns default placeholder`() {
        val result = underTest(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            nodeName = null,
        )

        assertThat(result).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result as LocalizedText.StringRes).resId)
            .isEqualTo(sharedR.string.search_placeholder_cloud_drive)
    }
}

