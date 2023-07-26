package mega.privacy.android.app.middlelayer.reporter

/**
 * Performance reporter class to collect performance data such as Traces.
 */
interface PerformanceReporter {

    /**
     * Measures the time it takes to run the [block]
     *
     * @param traceName     Trace name to be uniquely identified
     */
    suspend fun <T> trace(traceName: String, block: suspend () -> T)

    /**
     * This will start a trace with the given name.
     * A trace is a report of performance data captured between two points in time.
     *
     * @param traceName     Trace name to be uniquely identified
     */
    fun startTrace(traceName: String)

    /**
     * Sets the value of the metric with the given name in a trace to the value provided.
     * If a metric with the given name doesn't exist, a new one will be created.
     * If the trace has not been started or has already been stopped,
     * returns immediately without taking action.
     *
     * @param traceName     Trace name associated with this metric
     * @param metricName    Metric name
     * @param value         Metric value
     */
    fun putMetric(traceName: String, metricName: String, value: Long)

    /**
     * Sets a String value for the specified attribute. Updates the value of the attribute
     * if the attribute already exists. If the trace has been stopped,
     * this method returns without adding the attribute.
     *
     * @param traceName     Trace name associated with this metric
     * @param attribute     Attribute name
     * @param value         Attribute value
     */
    fun putAttribute(traceName: String, attribute: String, value: String)

    /**
     * Stops a trace given a Trace name.
     *
     * @param traceName     Trace name to be stopped
     */
    fun stopTrace(traceName: String)

    /**
     * Stops and clear all the existing traces
     */
    fun clearTraces()

    /**
     * Set if allow to collect and upload performance info.
     *
     * @param enabled true if allowed, false otherwise.
     */
    fun setEnabled(enabled: Boolean)
}
