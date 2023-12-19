package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.monitoring.EnablePerformanceReporterUseCase
import javax.inject.Inject

/**
 * Use case to update crash and performance reporters
 *
 * @property getCookieSettingsUseCase                    Get cookie settings use case
 * @property enablePerformanceReporterUseCase            Enable performance reporter use case
 * @property crashReporter                               Crash reporter
 */
class UpdateCrashAndPerformanceReportersUseCase @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val enablePerformanceReporterUseCase: EnablePerformanceReporterUseCase,
    private val crashReporter: CrashReporter,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() {
        val isEnabled = getCookieSettingsUseCase().contains(CookieType.ANALYTICS)
        crashReporter.setEnabled(isEnabled)
        enablePerformanceReporterUseCase(isEnabled)
    }
}