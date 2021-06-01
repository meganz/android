package mega.privacy.android.app.fragments.managerFragments.cu

import nz.mega.sdk.MegaNode
import java.io.File

data class CUCard(
    val node: MegaNode,
    var preview: File?,
    var day: String? = null,
    var month: String? = null,
    val year: String,
    val date: String,
    var numItems: Long? = null
)
