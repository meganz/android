package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * OpenShareDialog use case. This gets called when user shares a node using bottom sheet dialog
 */
@Deprecated(
    message = "This use-case has been deprecated in favour of a version that follows our domain",
    replaceWith = ReplaceWith(
        expression = "mega.privacy.android.domain.usecase.shares.CreateShareKey"
    ),
    level = DeprecationLevel.WARNING
)
fun interface CreateShareKey {

    /**
     * Invoke
     *
     * @param node : [MegaNode]
     */
    suspend operator fun invoke(node: MegaNode)
}