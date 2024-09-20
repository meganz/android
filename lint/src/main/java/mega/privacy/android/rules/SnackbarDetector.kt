package mega.privacy.android.rules

import mega.privacy.android.rules.compose.Material2InsteadOfDesignSystemComponentDetector

/**
 * Snackbar detector
 */
internal class SnackbarDetector : Material2InsteadOfDesignSystemComponentDetector(
    discouragedComponentName = "Snackbar",
    encouragedComponentName = "MegaSnackbar",
) {

    companion object {
        /**
         * Issue
         */
        val ISSUE = SnackbarDetector().createIssue()
    }
}