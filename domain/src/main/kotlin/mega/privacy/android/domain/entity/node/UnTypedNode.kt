package mega.privacy.android.domain.entity.node

import kotlinx.serialization.Polymorphic

/**
 * Un typed node - Interface used by the data layer
 */
@Polymorphic
sealed interface UnTypedNode : Node