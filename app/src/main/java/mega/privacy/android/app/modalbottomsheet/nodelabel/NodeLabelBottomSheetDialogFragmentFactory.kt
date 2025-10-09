package mega.privacy.android.app.modalbottomsheet.nodelabel

import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory class for creating NodeLabelBottomSheetDialogFragment instances.
 * Uses feature flag to decide between Java version (v1) and Kotlin version (v2).
 */
@Singleton
class NodeLabelBottomSheetDialogFragmentFactory @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    /**
     * Creates a new instance of the Fragment for a single node
     *
     * @param nodeHandle The handle of the node
     * @return A new Fragment instance (either v1 Java or v2 Kotlin based on feature flag)
     */
    suspend fun newInstance(nodeHandle: Long): BaseBottomSheetDialogFragment {
        return if (getFeatureFlagValueUseCase(AppFeatures.NodeLabelBottomSheetDialogFragmentConversion)) {
            NodeLabelBottomSheetDialogFragmentV2.newInstance(nodeHandle)
        } else {
            NodeLabelBottomSheetDialogFragment.newInstance(nodeHandle)
        }
    }

    /**
     * Creates a new instance of the Fragment for multiple nodes
     *
     * @param nodeHandles Array of node handles
     * @return A new Fragment instance (either v1 Java or v2 Kotlin based on feature flag)
     */
    suspend fun newInstance(nodeHandles: LongArray?): BaseBottomSheetDialogFragment {
        return if (getFeatureFlagValueUseCase(AppFeatures.NodeLabelBottomSheetDialogFragmentConversion)) {
            NodeLabelBottomSheetDialogFragmentV2.newInstance(nodeHandles)
        } else {
            NodeLabelBottomSheetDialogFragment.newInstance(nodeHandles)
        }
    }

    /**
     * Creates a new instance of the Fragment for multiple nodes (List version)
     *
     * @param nodeHandles List of node handles
     * @return A new Fragment instance (either v1 Java or v2 Kotlin based on feature flag)
     */
    suspend fun newInstance(nodeHandles: List<Long>): BaseBottomSheetDialogFragment {
        return if (getFeatureFlagValueUseCase(AppFeatures.NodeLabelBottomSheetDialogFragmentConversion)) {
            NodeLabelBottomSheetDialogFragmentV2.newInstance(nodeHandles)
        } else {
            NodeLabelBottomSheetDialogFragment.newInstance(nodeHandles)
        }
    }
}
