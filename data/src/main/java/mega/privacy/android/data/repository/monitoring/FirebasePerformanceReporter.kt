package mega.privacy.android.data.repository.monitoring

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.ktx.trace
import com.google.firebase.perf.metrics.Trace
import mega.privacy.android.domain.repository.monitoring.PerformanceReporterRepository
import java.util.Hashtable
import javax.inject.Inject

/**
 * Report performance to FirebasePerformance
 */
internal class PerformanceReporterRepositoryImpl @Inject constructor(
    private val firebasePerformance: FirebasePerformance,
) : PerformanceReporterRepository {

    /*
     * thread safe trace container
     */

    private val traces = Hashtable<String, Trace>()

    override suspend fun <T> trace(traceName: String, block: suspend () -> T): T {
        stopTrace(traceName)
        return firebasePerformance.newTrace(traceName).run {
            traces[traceName] = this
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

    override fun stopTraces(traceNames: List<String>) {
        traceNames.forEach { name -> traces[name]?.stop() }
    }

    override fun setEnabled(enabled: Boolean) {
        firebasePerformance.isPerformanceCollectionEnabled = enabled
    }
}
