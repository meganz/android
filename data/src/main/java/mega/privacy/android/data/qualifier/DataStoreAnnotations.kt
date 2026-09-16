package mega.privacy.android.data.qualifier

import javax.inject.Qualifier

/**
 * Annotation for RequestPhoneNumberPreferencesDataStore
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class RequestPhoneNumberPreference

/**
 * Annotation for ContinueWhereLeftOffSortPreferenceDataStore
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class ContinueWhereLeftOffSortPreference

/**
 * Annotation for ViewedLinksSortPreferenceDataStore
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class ViewedLinksSortPreference

/**
 * Annotation for PinnedItemsSortPreferenceDataStore
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class PinnedItemsSortPreference

/**
 * Annotation for PaymentDatastore
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class PaymentPreference

/**
 * Annotation for PushesPreferencesDataStore
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class PushesPreference
