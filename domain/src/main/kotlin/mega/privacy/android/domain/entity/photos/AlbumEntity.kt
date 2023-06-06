package mega.privacy.android.domain.entity.photos

import mega.privacy.android.domain.entity.set.UserSet

typealias UserSetPhotoIds = Pair<UserSet, List<AlbumPhotoId>>

typealias AlbumPhotoIds = Pair<Album.UserAlbum, List<AlbumPhotoId>>

typealias AlbumIdLink = Pair<AlbumId, AlbumLink>

@JvmInline
value class AlbumLink(val link: String)
