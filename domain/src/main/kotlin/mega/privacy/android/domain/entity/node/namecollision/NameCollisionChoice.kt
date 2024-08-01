package mega.privacy.android.domain.entity.node.namecollision

import kotlinx.serialization.Serializable

/**
 * Enum class for defining the type of resolution for a collision.
 */
@Serializable
enum class NameCollisionChoice { REPLACE_UPDATE_MERGE, CANCEL, RENAME }