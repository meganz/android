package mega.privacy.android.core.nodecomponents.menu.provider

import com.google.common.truth.Truth.assertThat
import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionsMenuOptionsProviderTest {

    private val bottomSheetOptions =
        mock<Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>>()
    private val selectionModeOptions =
        mock<Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>>>()

    private lateinit var underTest: VersionsMenuOptionsProvider

    @BeforeEach
    fun setUp() {
        reset(bottomSheetOptions, selectionModeOptions)
        underTest = VersionsMenuOptionsProvider(
            bottomSheetOptions = bottomSheetOptions,
            selectionModeOptions = selectionModeOptions,
        )
    }

    @Test
    fun `test that supportedSourceType is VERSIONS`() {
        assertThat(underTest.supportedSourceType).isEqualTo(NodeSourceType.VERSIONS)
    }

    @Test
    fun `test that getBottomSheetOptions returns the lazy set`() {
        val expected = setOf<NodeBottomSheetMenuItem<MenuActionWithIcon>>()
        whenever(bottomSheetOptions.get()).thenReturn(expected)

        assertThat(underTest.getBottomSheetOptions()).isEqualTo(expected)
    }

    @Test
    fun `test that getSelectionModeOptions returns the lazy set`() {
        val expected = setOf<NodeSelectionMenuItem<MenuActionWithIcon>>()
        whenever(selectionModeOptions.get()).thenReturn(expected)

        assertThat(underTest.getSelectionModeOptions()).isEqualTo(expected)
    }
}
