package mega.privacy.android.app.service.reporter

import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter

/**
 * TODO Implement App Gallery Automatic Performance Monitoring
 * https://developer.huawei.com/consumer/en/agconnect/apm/
 */
class AppGalleryPerformanceReporter : PerformanceReporter {

    override fun startTrace(traceName: String) {
        // Do nothing
    }

    override fun putMetric(traceName: String, metricName: String, value: Long) {
        // Do nothing
    }

    override fun putAttribute(traceName: String, attribute: String, value: String) {
        // Do nothing
    }

    override fun stopTrace(traceName: String) {
        // Do nothing
    }

    override fun clearTraces() {
        // Do nothing
    }

    override fun setEnabled(enabled: Boolean) {
        // Do nothing
    }
}
