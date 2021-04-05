package mega.privacy.android.app.di

import javax.inject.Qualifier

/** Annotation for an folder link MegaApiAndroid dependency. */
@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class MegaApiFolder
