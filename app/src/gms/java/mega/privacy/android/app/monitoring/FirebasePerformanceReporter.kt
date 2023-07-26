package mega.privacy.android.app.monitoring

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.ktx.trace
import com.google.firebase.perf.metrics.Trace
import mega.privacy.android.app.monitoring.PerformanceReporter

/**
 * Report performance to FirebasePerformance
 */
class FirebasePerformanceReporter(
    private val firebasePerformance: FirebasePerformance
) : PerformanceReporter {

    private val traces = mutableMapOf<String, Trace>()

    override suspend fun <T> trace(traceName: String, block: suspend () -> T) {
        stopTrace(traceName)
        traces[traceName] = firebasePerformance.newTrace(traceName).apply {
            trace { block() }
        }
    }

    override fun startTrace(traceName: String) {
        stopTrace(traceName)
        traces[traceName] = firebasePerformance.newTrace(traceName).apply { start() }
    }

    override fun putMetric(traceName: String, metricName: String, value: Long) {
        traces[traceName]?.putMetric(metricName, value)
    }

    override fun putAttribute(traceName: String, attribute: String, value: String) {
        traces[traceName]?.putAttribute(attribute, value)
    }

    override fun stopTrace(traceName: String) {
        traces[traceName]?.stop()
    }

    override fun clearTraces() {
        traces.values.forEach(Trace::stop)
        traces.clear()
    }

    override fun setEnabled(enabled: Boolean) {
        firebasePerformance.isPerformanceCollectionEnabled = enabled
    }
}
