package mega.privacy.android.app.di.mediaplayer

import javax.inject.Qualifier

/**
 * Annotation for the revamped Compose video player's own player gateway.
 *
 * Deliberately separate from [VideoPlayer], which is a `@Singleton` shared by
 * [mega.privacy.android.app.presentation.videoplayer.VideoPlayerActivity] and
 * [mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2]. The Compose player
 * gets an unscoped instance so the two implementations never drive the same ExoPlayer while
 * [mega.privacy.android.feature_flags.AppFeatures.VideoPlayerActivityRefactor] can route to
 * either of them.
 *
 * Its provider returns [mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway] directly,
 * with no intermediate `@Binds`. [VideoPlayer] and [AudioPlayer] still take the two-step route, so
 * the three are not symmetric. That is deliberate: every consumer of those two is already on a
 * deletion list — `VideoPlayerActivity` and `VideoPlayerViewModelV2` go when the refactor flag is
 * cut, and the `@AudioPlayer` consumers are the legacy audio player the revamped one replaces.
 * Reshaping DI for code that is on its way out buys nothing.
 */
@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class ComposeVideoPlayer
