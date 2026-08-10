package mega.privacy.android.feature.documentscanner.di

import javax.inject.Qualifier

/**
 * Marks the [okhttp3.OkHttpClient] used to fetch the boundary-detection model.
 *
 * The provided client has timeouts tuned for a large single-shot download
 * (~93 MB, minutes-long read/call budgets), so it is intentionally NOT a
 * good fit for general-purpose API calls. Anything else that needs HTTP
 * should provide its own client.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ScannerModelHttpClient
