package mega.privacy.android.app.presentation.favourites.model

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode


/**
 * Id - PlaceHolder extension until refactor is complete
 */
val TypedNode.id get() = (this as? TypedFileNode)?.id ?: (this as? TypedFolderNode)?.id