package mega.privacy.android.domain.entity.node.publiclink

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Public link node
 *
 * Marks a node as belonging to a public link, so that it is resolved through the folder link API
 * rather than the account API.
 *
 * @constructor Create empty Public link node
 */
sealed interface PublicLinkNode : TypedNode
