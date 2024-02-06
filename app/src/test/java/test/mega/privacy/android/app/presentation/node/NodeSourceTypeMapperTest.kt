package test.mega.privacy.android.app.presentation.node

import com.google.common.truth.Truth
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.node.NodeSourceTypeMapper
import mega.privacy.android.domain.entity.node.NodeSourceType
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSourceTypeMapperTest {
    private val underTest: NodeSourceTypeMapper = NodeSourceTypeMapper()

    @ParameterizedTest(name = "test that drawer item {0}, shares tab {1} is mapped correctly to Search type.{2}")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(
        selectedDrawerItem: DrawerItem,
        selectedSharesTab: SharesTab,
        expected: NodeSourceType,
    ) {
        val actual = underTest(selectedDrawerItem, selectedSharesTab)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(DrawerItem.CLOUD_DRIVE, SharesTab.NONE, NodeSourceType.CLOUD_DRIVE),
        Arguments.of(DrawerItem.BACKUPS, SharesTab.NONE, NodeSourceType.BACKUPS),
        Arguments.of(DrawerItem.RUBBISH_BIN, SharesTab.NONE, NodeSourceType.RUBBISH_BIN),
        Arguments.of(DrawerItem.HOMEPAGE, SharesTab.NONE, NodeSourceType.CLOUD_DRIVE),
        Arguments.of(DrawerItem.SHARED_ITEMS, SharesTab.LINKS_TAB, NodeSourceType.LINKS),
        Arguments.of(
            DrawerItem.SHARED_ITEMS,
            SharesTab.OUTGOING_TAB,
            NodeSourceType.OUTGOING_SHARES
        ),
        Arguments.of(
            DrawerItem.SHARED_ITEMS,
            SharesTab.INCOMING_TAB,
            NodeSourceType.INCOMING_SHARES
        ),
        Arguments.of(DrawerItem.SHARED_ITEMS, SharesTab.NONE, NodeSourceType.OTHER),
    )
}