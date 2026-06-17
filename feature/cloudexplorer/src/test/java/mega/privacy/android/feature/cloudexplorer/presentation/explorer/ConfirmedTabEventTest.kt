package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedChatButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedCloudButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedFavouritesButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedIncomingButtonPressedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ConfirmedTabEventTest {

    @Test
    fun `test that confirmedTabEvent returns chat event when chat tab is selected`() {
        assertThat(
            confirmedTabEvent(CHAT_TAB_INDEX, NodeSourceType.CLOUD_DRIVE)
        ).isEqualTo(CloudExplorerConfirmedChatButtonPressedEvent)
    }

    @ParameterizedTest(name = "tab {0} and source {1} maps to expected event")
    @MethodSource("tabAndSourceCases")
    fun `test that confirmedTabEvent maps tab and node source to the expected event`(
        selectedTabIndex: Int,
        nodeSourceType: NodeSourceType,
        expectedEvent: EventIdentifier,
    ) {
        assertThat(confirmedTabEvent(selectedTabIndex, nodeSourceType)).isEqualTo(expectedEvent)
    }

    companion object {
        @JvmStatic
        private fun tabAndSourceCases() = listOf(
            // Top level: the selected tab drives the choice.
            Arguments.of(
                CLOUD_TAB_INDEX,
                NodeSourceType.CLOUD_DRIVE,
                CloudExplorerConfirmedCloudButtonPressedEvent,
            ),
            Arguments.of(
                INCOMING_TAB_INDEX,
                NodeSourceType.CLOUD_DRIVE,
                CloudExplorerConfirmedIncomingButtonPressedEvent,
            ),
            Arguments.of(
                FAVOURITES_TAB_INDEX,
                NodeSourceType.CLOUD_DRIVE,
                CloudExplorerConfirmedFavouritesButtonPressedEvent,
            ),
            // Inner navigation hides tabs (index resets to cloud): node source is the fallback.
            Arguments.of(
                CLOUD_TAB_INDEX,
                NodeSourceType.INCOMING_SHARES,
                CloudExplorerConfirmedIncomingButtonPressedEvent,
            ),
            Arguments.of(
                CLOUD_TAB_INDEX,
                NodeSourceType.FAVOURITES,
                CloudExplorerConfirmedFavouritesButtonPressedEvent,
            ),
        )
    }
}
