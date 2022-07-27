package mega.privacy.android.app.di.mediaplayer

import javax.inject.Qualifier

/** Annotation for video player MediaPlayerFacade dependency. */
@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class VideoPlayer
