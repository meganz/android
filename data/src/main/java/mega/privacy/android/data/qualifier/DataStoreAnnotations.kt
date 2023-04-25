package mega.privacy.android.data.qualifier

import javax.inject.Qualifier

/**
 * Annotation for CameraTimestampsPreferenceDataStore
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class CameraTimestampsPreference

/**
 * Annotation for RequestPhoneNumberPreferencesDataStore
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class RequestPhoneNumberPreference
