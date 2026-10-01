package mega.privacy.android.domain.entity.node

import kotlinx.serialization.Serializable

/**
 * NodeId id value class
 *
 * @property longValue
 */
@JvmInline
@Serializable
value class NodeId(val longValue: Long)