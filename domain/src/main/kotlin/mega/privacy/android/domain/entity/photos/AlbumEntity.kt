package mega.privacy.android.domain.entity.photos

import mega.privacy.android.domain.entity.set.UserSet

typealias UserSetPhotoIds = Pair<UserSet, List<AlbumPhotoId>>

typealias AlbumPhotos = Pair<Album.UserAlbum, List<Photo>>

typealias AlbumIdLink = Pair<AlbumId, AlbumLink>

@JvmInline
value class AlbumLink(val link: String)
