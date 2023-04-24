package mega.privacy.android.domain.entity.photos

typealias AlbumIdPhotoIds = Pair<AlbumId, List<AlbumPhotoId>>

typealias AlbumPhotos = Pair<Album.UserAlbum, List<Photo>>

typealias AlbumIdLink = Pair<AlbumId, AlbumLink>

@JvmInline
value class AlbumLink(val link: String)
