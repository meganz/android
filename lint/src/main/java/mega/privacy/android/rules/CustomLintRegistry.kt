package mega.privacy.android.rules

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

/**
 * Custom lint registry
 *
 */
@Suppress("UnstableApiUsage")
class CustomLintRegistry : IssueRegistry() {
    override val issues =
        listOf(CallIsSystemInDarkThemeIssue.ISSUE)

    override val api: Int = CURRENT_API

    override val minApi: Int = 9

    override val vendor: Vendor = Vendor(
        vendorName = "Mega",
        feedbackUrl = "https://mega.co.nz",
        contact = "https://mega.co.nz"
    )
}