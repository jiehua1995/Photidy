package app.photidy.core.designsystem.artwork

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.photidy.core.model.PhotoItem
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Unified photo rendering: real media is decoded by Coil (video shows its first frame),
 * while procedural demo photos keep using the offline [PhotoArtwork] painter.
 */
@Composable
fun PhotoImage(
    photo: PhotoItem,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
) {
    val uri = photo.contentUri
    if (uri.isNullOrBlank()) {
        PhotoArtwork(
            photo = photo,
            modifier = modifier,
            contentDescription = contentDescription,
        )
        return
    }

    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(Uri.parse(uri))
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}
