package mega.privacy.android.rules

import mega.privacy.android.rules.compose.Material2InsteadOfDesignSystemComponentDetector

/**
 * Scaffold detector
 */
internal class ScaffoldDetector : Material2InsteadOfDesignSystemComponentDetector(
    discouragedComponentName = "Scaffold",
    encouragedComponentName = "MegaScaffold",
) {

    companion object {
        /**
         * Issue
         */
        val ISSUE = ScaffoldDetector().createIssue()
    }
}