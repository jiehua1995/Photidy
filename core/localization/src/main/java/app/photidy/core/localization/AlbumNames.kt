package app.photidy.core.localization

/** Stable mapping from album ids to localized names. */
object AlbumNames {
    fun nameRes(albumId: String): Int = when (albumId) {
        "camera" -> R.string.album_camera
        "travel" -> R.string.album_travel
        "family" -> R.string.album_family
        "nature" -> R.string.album_nature
        "city" -> R.string.album_city
        "food" -> R.string.album_food
        "pets" -> R.string.album_pets
        else -> R.string.album_other
    }
}
