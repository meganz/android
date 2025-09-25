package mega.privacy.android.core.nodecomponents.menu.provider

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * Strategy interface for providing node options based on source type.
 * This follows the Strategy pattern to eliminate the need for when statements
 * and makes the code more extensible and testable.
 */
interface NodeMenuOptionsProvider {
    /**
     * Returns the supported source type for this provider
     */
    val supportedSourceType: NodeSourceType

    /**
     * Provides bottom sheet menu items for the supported source type
     */
    fun getBottomSheetOptions(): Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>

    /**
     * Provides selection mode menu items for the supported source type
     */
    fun getSelectionModeOptions(): Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>
}
