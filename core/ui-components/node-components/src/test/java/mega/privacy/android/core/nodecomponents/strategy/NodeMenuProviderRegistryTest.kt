package mega.privacy.android.core.nodecomponents.strategy

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.provider.NodeMenuOptionsProvider
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistryImpl
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeMenuProviderRegistryTest {
    private lateinit var underTest: NodeMenuProviderRegistryImpl
    private var mockProvider: NodeMenuOptionsProvider = mock()

    @BeforeAll
    fun setUp() {
        underTest = NodeMenuProviderRegistryImpl(setOf(mockProvider))
    }

    @AfterEach
    fun resetMocks() {
        reset(mockProvider)
    }

    @Test
    fun `test that getBottomSheetOptions should return correct options for supported source type`() {
        val expectedOptions = setOf<NodeBottomSheetMenuItem<MenuActionWithIcon>>()

        whenever(mockProvider.supportedSourceType).thenReturn(NodeSourceType.CLOUD_DRIVE)
        whenever(mockProvider.getBottomSheetOptions()).thenReturn(expectedOptions)

        val result = underTest.getBottomSheetOptions(NodeSourceType.CLOUD_DRIVE)

        assertThat(result).isEqualTo(expectedOptions)
    }

    @Test
    fun `test that getBottomSheetOptions should return empty set for unsupported source type`() {
        whenever(mockProvider.supportedSourceType).thenReturn(NodeSourceType.CLOUD_DRIVE)

        val result = underTest.getBottomSheetOptions(NodeSourceType.RUBBISH_BIN)

        assertThat(result).isEmpty()
    }

    @Test
    fun `getSelectionModeOptions should return correct options for supported source type`() {
        val expectedOptions = setOf<NodeSelectionMenuItem<MenuActionWithIcon>>()

        whenever(mockProvider.supportedSourceType).thenReturn(NodeSourceType.RUBBISH_BIN)
        whenever(mockProvider.getSelectionModeOptions()).thenReturn(expectedOptions)

        val result = underTest.getSelectionModeOptions(NodeSourceType.RUBBISH_BIN)

        assertThat(result).isEqualTo(expectedOptions)
    }

    @Test
    fun `test that getSelectionModeOptions should return empty set for unsupported source type`() {
        whenever(mockProvider.supportedSourceType).thenReturn(NodeSourceType.CLOUD_DRIVE)

        val result = underTest.getSelectionModeOptions(NodeSourceType.RUBBISH_BIN)

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that isSourceTypeSupported should return true for supported source type`() {
        whenever(mockProvider.supportedSourceType).thenReturn(NodeSourceType.CLOUD_DRIVE)

        val result = underTest.isSourceTypeSupported(NodeSourceType.CLOUD_DRIVE)

        assertThat(result).isTrue()
    }

    @Test
    fun `test that isSourceTypeSupported should return false for unsupported source type`() {
        whenever(mockProvider.supportedSourceType).thenReturn(NodeSourceType.CLOUD_DRIVE)

        val result = underTest.isSourceTypeSupported(NodeSourceType.RUBBISH_BIN)

        assertThat(result).isFalse()
    }
}
