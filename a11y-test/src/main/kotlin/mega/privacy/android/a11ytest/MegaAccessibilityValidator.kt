package mega.privacy.android.a11ytest

import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.ImageContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TextSizeCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.Matcher

/**
 * Builds an [AccessibilityValidator] implementing the MEGA accessibility test policy.
 *
 * Developer-owned findings (missing labels, undersized touch targets, ...) throw an
 * `AccessibilityViewCheckException` so the test fails. Design-owned findings (text and image
 * contrast, text size) are suppressed so they never fail a test, but every result — the
 * suppressed ones included — is forwarded to [reporter] and serialized to a JSON report that
 * can be collected from the host after the run.
 *
 * Enable it on a Compose test rule via
 * `composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))` and trigger
 * checks with `onRoot().tryPerformAccessibilityChecks()`.
 *
 * @param reporter receives every check result for reporting; keep a reference to it to assert
 * on recorded findings.
 * @param baselineSuppressions optional matcher for known pre-existing findings in components
 * that cannot be fixed in this repository (e.g. the prebuilt core-ui artifact). Matched results
 * are recorded as `SUPPRESSED` instead of failing the test. Keep each matcher as narrow as
 * possible and scoped to the test that mounts the affected component: the baseline may only
 * ratchet down — never widen a suppression to let new findings in, and remove it once the
 * owning library fixes the finding.
 */
fun megaAccessibilityValidator(
    reporter: AccessibilityCheckReporter = AccessibilityCheckReporter(),
    baselineSuppressions: Matcher<AccessibilityCheckResult>? = null,
): AccessibilityValidator = AccessibilityValidator()
    .setThrowExceptionForErrors(true)
    .setCaptureScreenshots(true)
    .setSuppressingResultMatcher(
        anyOf(
            listOfNotNull(
                matchesCheck(TextContrastCheck::class.java),
                matchesCheck(ImageContrastCheck::class.java),
                matchesCheck(TextSizeCheck::class.java),
                baselineSuppressions,
            )
        )
    )
    .addCheckListener(reporter)
