package mega.privacy.android.app.presentation.node

import com.google.common.truth.Truth
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.HomepageScreen
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.domain.entity.node.NodeSourceType
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSourceTypeMapperTest {
    private val underTest: NodeSourceTypeMapper = NodeSourceTypeMapper()

    @ParameterizedTest(name = "test that drawer item {0}, homepage screen tab {1}, shares tab {2} is mapped correctly to Search type.{3}")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(
        selectedDrawerItem: DrawerItem,
        selectedHomepageScreen: HomepageScreen,
        selectedSharesTab: SharesTab,
        expected: NodeSourceType,
    ) {
        val actual = underTest(selectedDrawerItem, selectedHomepageScreen, selectedSharesTab)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            DrawerItem.HOMEPAGE,
            HomepageScreen.HOMEPAGE,
            SharesTab.NONE,
            NodeSourceType.HOME
        ),
        Arguments.of(
            DrawerItem.HOMEPAGE,
            HomepageScreen.FAVOURITES,
            SharesTab.NONE,
            NodeSourceType.FAVOURITES
        ),
        Arguments.of(
            DrawerItem.CLOUD_DRIVE,
            HomepageScreen.HOMEPAGE,
            SharesTab.NONE,
            NodeSourceType.CLOUD_DRIVE
        ),
        Arguments.of(
            DrawerItem.BACKUPS,
            HomepageScreen.HOMEPAGE,
            SharesTab.NONE,
            NodeSourceType.BACKUPS
        ),
        Arguments.of(
            DrawerItem.RUBBISH_BIN,
            HomepageScreen.HOMEPAGE,
            SharesTab.NONE,
            NodeSourceType.RUBBISH_BIN
        ),
        Arguments.of(
            DrawerItem.SHARED_ITEMS,
            HomepageScreen.HOMEPAGE,
            SharesTab.LINKS_TAB,
            NodeSourceType.LINKS
        ),
        Arguments.of(
            DrawerItem.SHARED_ITEMS,
            HomepageScreen.HOMEPAGE,
            SharesTab.OUTGOING_TAB,
            NodeSourceType.OUTGOING_SHARES
        ),
        Arguments.of(
            DrawerItem.SHARED_ITEMS,
            HomepageScreen.HOMEPAGE,
            SharesTab.INCOMING_TAB,
            NodeSourceType.INCOMING_SHARES
        ),
        Arguments.of(
            DrawerItem.SHARED_ITEMS,
            HomepageScreen.HOMEPAGE,
            SharesTab.NONE,
            NodeSourceType.OTHER
        )
    )
}