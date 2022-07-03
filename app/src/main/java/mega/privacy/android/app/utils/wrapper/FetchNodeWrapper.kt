package mega.privacy.android.app.utils.wrapper

import nz.mega.sdk.MegaNode

fun interface FetchNodeWrapper {
    suspend operator fun invoke(nodeId: Long): MegaNode?
}