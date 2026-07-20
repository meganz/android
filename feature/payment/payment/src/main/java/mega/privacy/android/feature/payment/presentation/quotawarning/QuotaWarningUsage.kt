package mega.privacy.android.feature.payment.presentation.quotawarning

internal const val BYTES_IN_GB = 1024L * 1024L * 1024L

/**
 * Usage of [usedBytes] against a plan quota of [quotaGb] gigabytes, as a 0..100 percentage.
 * Used only for the recommended-plan card projection bar. Returns 0 when the inputs are missing
 * or the quota is non-positive.
 */
internal fun usagePercentageAgainstQuota(usedBytes: Long?, quotaGb: Int?): Float {
    if (usedBytes == null || quotaGb == null || quotaGb <= 0) return 0f
    val quotaBytes = quotaGb.toLong() * BYTES_IN_GB
    return (usedBytes.toFloat() / quotaBytes.toFloat() * 100f).coerceIn(0f, 100f)
}
