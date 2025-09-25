package mega.privacy.android.core.nodecomponents.menu.registry

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * Interface for managing node options based on source type.
 * This provides a clean abstraction for retrieving options without
 * depending on concrete implementations.
 */
interface NodeMenuProviderRegistry {

    /**
     * Gets bottom sheet options for the specified source type
     */
    fun getBottomSheetOptions(sourceType: NodeSourceType): Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<*>>

    /**
     * Gets selection mode options for the specified source type
     */
    fun getSelectionModeOptions(sourceType: NodeSourceType): Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>

    /**
     * Checks if a source type is supported
     */
    fun isSourceTypeSupported(sourceType: NodeSourceType): Boolean
}