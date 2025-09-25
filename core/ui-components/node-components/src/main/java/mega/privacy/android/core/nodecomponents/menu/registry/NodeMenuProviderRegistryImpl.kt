package mega.privacy.android.core.nodecomponents.menu.registry

import mega.privacy.android.core.nodecomponents.menu.provider.NodeMenuOptionsProvider
import mega.privacy.android.domain.entity.node.NodeSourceType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of NodeOptionsRegistry that manages all NodeOptionsProvider implementations.
 * This follows the Registry pattern to provide a clean abstraction
 * for retrieving options based on source type.
 */
@Singleton
class NodeMenuProviderRegistryImpl @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards NodeMenuOptionsProvider>,
) : NodeMenuProviderRegistry {

    private val providerMap: Map<NodeSourceType, NodeMenuOptionsProvider> by lazy {
        providers.associateBy { it.supportedSourceType }
    }

    /**
     * Gets bottom sheet options for the specified source type
     */
    override fun getBottomSheetOptions(sourceType: NodeSourceType) =
        providerMap[sourceType]?.getBottomSheetOptions() ?: emptySet()

    /**
     * Gets selection mode options for the specified source type
     */
    override fun getSelectionModeOptions(sourceType: NodeSourceType) =
        providerMap[sourceType]?.getSelectionModeOptions() ?: emptySet()

    /**
     * Checks if a source type is supported
     */
    override fun isSourceTypeSupported(sourceType: NodeSourceType) =
        providerMap.containsKey(sourceType)
}
