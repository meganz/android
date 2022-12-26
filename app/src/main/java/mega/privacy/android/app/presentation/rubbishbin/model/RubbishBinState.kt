package mega.privacy.android.app.presentation.rubbishbin.model

import nz.mega.sdk.MegaNode

/**
 *  @property rubbishBinHandle The current RubbishBin Handle
 *  @property nodes List of RubbishBin Nodes
 */
data class RubbishBinState(
    val rubbishBinHandle: Long = -1L,
    val nodes: List<MegaNode> = emptyList()
)
