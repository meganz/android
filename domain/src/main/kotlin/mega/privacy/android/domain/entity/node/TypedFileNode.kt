package mega.privacy.android.domain.entity.node

import kotlinx.serialization.Polymorphic

/**
 * Typed file node
 */
@Polymorphic
interface TypedFileNode : TypedNode, FileNode