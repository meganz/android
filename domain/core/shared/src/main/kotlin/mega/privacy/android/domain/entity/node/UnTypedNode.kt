package mega.privacy.android.domain.entity.node

import kotlinx.serialization.Serializable

/**
 * Un typed node - Interface used by the data layer
 */
@Serializable
sealed interface UnTypedNode : Node