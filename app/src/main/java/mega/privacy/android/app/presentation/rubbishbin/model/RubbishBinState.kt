package mega.privacy.android.app.presentation.rubbishbin.model

import nz.mega.sdk.MegaNode

/**
 *  @property rubbishBinHandle The current RubbishBin Handle
 *  @property nodes List of RubbishBin Nodes
 *  @property parentHandle parent handle of the current node
 */
data class RubbishBinState(
    val rubbishBinHandle: Long = -1L,
    val nodes: List<MegaNode> = emptyList(),
    val parentHandle: Long? = null,
)
