package app.photidy.core.designsystem.artwork

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.photidy.core.model.PhotoItem
import app.photidy.core.model.PhotoScene
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Procedural demo artwork. Real thumbnails will be supplied by Coil once MediaStore
 * integration lands; until then this keeps the prototype fully offline and stable.
 * Drawing is GPU-friendly (gradients and simple paths) and never blocks the main thread
 * with bitmap decoding.
 */
@Composable
fun PhotoArtwork(
    photo: PhotoItem,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Canvas(
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
        },
    ) {
        drawDemoScene(photo.scene, photo.paletteIndex, photo.artSeed)
    }
}

private data class ScenePalette(
    val skyTop: Color,
    val skyBottom: Color,
    val mid: Color,
    val deep: Color,
    val highlight: Color,
)

private val palettes = listOf(
    ScenePalette(
        skyTop = Color(0xFFF7E4D2),
        skyBottom = Color(0xFFF0C9AE),
        mid = Color(0xFF95A2AD),
        deep = Color(0xFF4E5D6C),
        highlight = Color(0xFFFFF4E6),
    ),
    ScenePalette(
        skyTop = Color(0xFFF5D8C8),
        skyBottom = Color(0xFFE8A78F),
        mid = Color(0xFF9B8FA6),
        deep = Color(0xFF4C4A66),
        highlight = Color(0xFFFFE9D8),
    ),
    ScenePalette(
        skyTop = Color(0xFFE9EFE7),
        skyBottom = Color(0xFFC3D3C6),
        mid = Color(0xFF8FA897),
        deep = Color(0xFF47604F),
        highlight = Color(0xFFF7FBF5),
    ),
    ScenePalette(
        skyTop = Color(0xFFF7E6CC),
        skyBottom = Color(0xFFEBC79D),
        mid = Color(0xFFC5936A),
        deep = Color(0xFF7F5A42),
        highlight = Color(0xFFFFF2DE),
    ),
    ScenePalette(
        skyTop = Color(0xFF222B3D),
        skyBottom = Color(0xFF3A4763),
        mid = Color(0xFF5C6A88),
        deep = Color(0xFF10141E),
        highlight = Color(0xFFF0C878),
    ),
    ScenePalette(
        skyTop = Color(0xFFEDF3F7),
        skyBottom = Color(0xFFCFDEE9),
        mid = Color(0xFF9EB7C9),
        deep = Color(0xFF617D93),
        highlight = Color(0xFFFFFFFF),
    ),
    ScenePalette(
        skyTop = Color(0xFFF4EFD8),
        skyBottom = Color(0xFFE3DDAE),
        mid = Color(0xFF9BB279),
        deep = Color(0xFF506B45),
        highlight = Color(0xFFFFF9E3),
    ),
    ScenePalette(
        skyTop = Color(0xFFDDECF2),
        skyBottom = Color(0xFFB5D0DE),
        mid = Color(0xFF7EA3B8),
        deep = Color(0xFF3E6276),
        highlight = Color(0xFFF4FBFF),
    ),
    ScenePalette(
        skyTop = Color(0xFFF1DEC4),
        skyBottom = Color(0xFFD9B48B),
        mid = Color(0xFFB07C53),
        deep = Color(0xFF6E4A35),
        highlight = Color(0xFFFFF1DC),
    ),
    ScenePalette(
        skyTop = Color(0xFFEDDCD8),
        skyBottom = Color(0xFFD6B7B6),
        mid = Color(0xFFAE8F91),
        deep = Color(0xFF6B5558),
        highlight = Color(0xFFFDF0EA),
    ),
)

private fun paletteFor(index: Int): ScenePalette = palettes[index.mod(palettes.size)]

private fun DrawScope.drawDemoScene(scene: PhotoScene, paletteIndex: Int, seed: Int) {
    val palette = paletteFor(paletteIndex)
    when (scene) {
        PhotoScene.MOUNTAIN_DAWN -> drawMountainDawn(palette, seed)
        PhotoScene.OCEAN_DUSK -> drawOceanDusk(palette, seed)
        PhotoScene.FOREST_MIST -> drawForestMist(palette, seed)
        PhotoScene.DESERT_DUNES -> drawDesertDunes(palette, seed)
        PhotoScene.CITY_NIGHT -> drawCityNight(palette, seed)
        PhotoScene.SNOW_PEAK -> drawSnowPeak(palette, seed)
        PhotoScene.MEADOW_LIGHT -> drawMeadow(palette, seed)
        PhotoScene.LAKE_REFLECTION -> drawLake(palette, seed)
        PhotoScene.WARM_TABLE -> drawWarmTable(palette, seed)
        PhotoScene.SOFT_PORTRAIT -> drawSoftPortrait(palette, seed)
    }
    drawGrain(seed)
    drawVignette()
}

private fun DrawScope.drawMountainDawn(palette: ScenePalette, seed: Int) {
    fillSky(palette)
    val sunCenter = Offset(size.width * 0.7f, size.height * 0.3f)
    val sunRadius = size.width * 0.055f
    drawSun(sunCenter, sunRadius, palette.highlight)

    drawMountainRange(
        baseY = size.height * 0.66f,
        amplitude = size.height * 0.2f,
        color = palette.mid.copy(alpha = 0.82f),
        seed = seed + 1,
    )
    drawMountainRange(
        baseY = size.height * 0.8f,
        amplitude = size.height * 0.16f,
        color = palette.deep.copy(alpha = 0.92f),
        seed = seed + 7,
    )

    // Soft haze over the valley.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, palette.highlight.copy(alpha = 0.24f), Color.Transparent),
            startY = size.height * 0.58f,
            endY = size.height * 0.82f,
        ),
    )
}

private fun DrawScope.drawOceanDusk(palette: ScenePalette, seed: Int) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.skyTop, palette.skyBottom),
            startY = 0f,
            endY = size.height * 0.62f,
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.highlight.copy(alpha = 0.55f), palette.mid, palette.deep),
            startY = size.height * 0.6f,
            endY = size.height,
        ),
    )

    val sunCenter = Offset(size.width * 0.66f, size.height * 0.58f)
    drawSun(sunCenter, size.width * 0.05f, palette.highlight)

    // Reflection column.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.highlight.copy(alpha = 0.5f),
                palette.highlight.copy(alpha = 0.08f),
                Color.Transparent,
            ),
            startY = size.height * 0.6f,
            endY = size.height,
        ),
        topLeft = Offset(size.width * 0.52f, size.height * 0.6f),
        size = Size(size.width * 0.28f, size.height * 0.4f),
    )

    val random = Random(seed + 3)
    repeat(26) { index ->
        val y = size.height * (0.64f + index * 0.013f)
        val lineWidth = size.width * (0.04f + random.nextFloat() * 0.16f)
        val x = size.width * (0.3f + random.nextFloat() * 0.45f)
        drawLine(
            color = palette.highlight.copy(alpha = 0.16f + random.nextFloat() * 0.12f),
            start = Offset(x, y),
            end = Offset(x + lineWidth, y),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
        )
    }

    // Dark shore at the bottom.
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height * 0.93f)
        cubicTo(
            size.width * 0.3f, size.height * 0.86f,
            size.width * 0.7f, size.height * 0.99f,
            size.width, size.height * 0.9f,
        )
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, palette.deep.copy(alpha = 0.85f))
}

private fun DrawScope.drawForestMist(palette: ScenePalette, seed: Int) {
    fillSky(palette)
    val random = Random(seed + 11)
    val treeCount = 9
    repeat(treeCount) { index ->
        val x = size.width * ((index + 0.5f) / treeCount) + (random.nextFloat() - 0.5f) * size.width * 0.05f
        val width = size.width * (0.008f + random.nextFloat() * 0.016f)
        val top = size.height * (0.12f + random.nextFloat() * 0.18f)
        drawRect(
            color = palette.deep.copy(alpha = 0.28f + random.nextFloat() * 0.25f),
            topLeft = Offset(x, top),
            size = Size(width, size.height - top),
        )
        if (index % 2 == 0) {
            drawLine(
                color = palette.deep.copy(alpha = 0.18f),
                start = Offset(x, top + size.height * 0.16f),
                end = Offset(x + width * 6f, top + size.height * 0.1f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.highlight.copy(alpha = 0.34f), Color.Transparent),
            startY = size.height * 0.2f,
            endY = size.height * 0.7f,
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, palette.deep.copy(alpha = 0.55f)),
            startY = size.height * 0.72f,
            endY = size.height,
        ),
    )
}

private fun DrawScope.drawDesertDunes(palette: ScenePalette, seed: Int) {
    fillSky(palette)
    drawSun(Offset(size.width * 0.28f, size.height * 0.28f), size.width * 0.06f, palette.highlight)

    drawDune(
        baseY = size.height * 0.6f,
        color = palette.mid.copy(alpha = 0.65f),
        offset = 0.1f,
    )
    drawDune(
        baseY = size.height * 0.72f,
        color = palette.mid.copy(alpha = 0.9f),
        offset = 0.55f,
    )
    drawDune(
        baseY = size.height * 0.86f,
        color = palette.deep.copy(alpha = 0.95f),
        offset = 0.85f,
    )

    val random = Random(seed + 5)
    repeat(5) {
        val y = size.height * (0.7f + random.nextFloat() * 0.2f)
        drawLine(
            color = palette.highlight.copy(alpha = 0.14f),
            start = Offset(size.width * random.nextFloat(), y),
            end = Offset(size.width * (0.6f + random.nextFloat() * 0.4f), y + 2f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawCityNight(palette: ScenePalette, seed: Int) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.skyTop, palette.skyBottom),
            startY = 0f,
            endY = size.height,
        ),
    )
    val random = Random(seed + 13)
    repeat(46) {
        drawCircle(
            color = palette.highlight.copy(alpha = 0.25f + random.nextFloat() * 0.5f),
            radius = random.nextFloat() * 1.6f + 0.6f,
            center = Offset(
                x = random.nextFloat() * size.width,
                y = random.nextFloat() * size.height * 0.5f,
            ),
        )
    }
    drawSun(Offset(size.width * 0.74f, size.height * 0.22f), size.width * 0.045f, palette.highlight)

    var x = 0f
    while (x < size.width) {
        val buildingWidth = size.width * (0.055f + random.nextFloat() * 0.07f)
        val buildingHeight = size.height * (0.16f + random.nextFloat() * 0.3f)
        drawRect(
            color = palette.deep.copy(alpha = 0.9f),
            topLeft = Offset(x, size.height - buildingHeight),
            size = Size(buildingWidth, buildingHeight),
        )
        var wy = size.height - buildingHeight + 10f
        while (wy < size.height - 14f) {
            var wx = x + 7f
            while (wx < x + buildingWidth - 7f) {
                if (random.nextFloat() > 0.45f) {
                    drawRect(
                        color = palette.highlight.copy(alpha = 0.55f),
                        topLeft = Offset(wx, wy),
                        size = Size(3f, 4f),
                    )
                }
                wx += 10f
            }
            wy += 13f
        }
        x += buildingWidth + size.width * 0.008f
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, palette.deep.copy(alpha = 0.85f)),
            startY = size.height * 0.78f,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height * 0.78f),
        size = Size(size.width, size.height * 0.22f),
    )
}

private fun DrawScope.drawSnowPeak(palette: ScenePalette, seed: Int) {
    fillSky(palette)
    drawSun(Offset(size.width * 0.24f, size.height * 0.22f), size.width * 0.045f, palette.highlight)

    drawMountainRange(
        baseY = size.height * 0.74f,
        amplitude = size.height * 0.28f,
        color = palette.mid.copy(alpha = 0.55f),
        seed = seed + 21,
    )
    drawMountainRange(
        baseY = size.height * 0.86f,
        amplitude = size.height * 0.18f,
        color = palette.highlight.copy(alpha = 0.9f),
        seed = seed + 29,
    )

    val random = Random(seed + 31)
    repeat(7) { index ->
        val treeX = size.width * (0.06f + index * 0.13f + random.nextFloat() * 0.03f)
        val treeBase = size.height * (0.93f + random.nextFloat() * 0.05f)
        val treeHeight = size.height * (0.1f + random.nextFloat() * 0.06f)
        val path = Path().apply {
            moveTo(treeX, treeBase - treeHeight)
            lineTo(treeX - treeHeight * 0.22f, treeBase)
            lineTo(treeX + treeHeight * 0.22f, treeBase)
            close()
        }
        drawPath(path, palette.deep.copy(alpha = 0.5f))
    }
}

private fun DrawScope.drawMeadow(palette: ScenePalette, seed: Int) {
    fillSky(palette)
    drawSun(Offset(size.width * 0.76f, size.height * 0.24f), size.width * 0.07f, palette.highlight)

    val hillPath = Path().apply {
        moveTo(0f, size.height * 0.68f)
        cubicTo(
            size.width * 0.3f, size.height * 0.58f,
            size.width * 0.6f, size.height * 0.76f,
            size.width, size.height * 0.62f,
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(hillPath, palette.mid.copy(alpha = 0.85f))

    val foreground = Path().apply {
        moveTo(0f, size.height * 0.82f)
        cubicTo(
            size.width * 0.35f, size.height * 0.74f,
            size.width * 0.65f, size.height * 0.9f,
            size.width, size.height * 0.8f,
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(foreground, palette.deep.copy(alpha = 0.8f))

    val random = Random(seed + 41)
    repeat(52) {
        val x = random.nextFloat() * size.width
        val y = size.height * (0.82f + random.nextFloat() * 0.17f)
        val length = size.height * (0.015f + random.nextFloat() * 0.03f)
        drawLine(
            color = palette.highlight.copy(alpha = 0.25f),
            start = Offset(x, y),
            end = Offset(x + length * 0.3f, y - length),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawLake(palette: ScenePalette, seed: Int) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.skyTop, palette.skyBottom),
            startY = 0f,
            endY = size.height * 0.56f,
        ),
    )
    drawMountainRange(
        baseY = size.height * 0.56f,
        amplitude = size.height * 0.22f,
        color = palette.mid.copy(alpha = 0.75f),
        seed = seed + 51,
    )
    drawMountainRange(
        baseY = size.height * 0.6f,
        amplitude = size.height * 0.13f,
        color = palette.deep.copy(alpha = 0.8f),
        seed = seed + 59,
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.mid.copy(alpha = 0.55f), palette.deep.copy(alpha = 0.95f)),
            startY = size.height * 0.6f,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height * 0.6f),
        size = Size(size.width, size.height * 0.4f),
    )

    val random = Random(seed + 61)
    repeat(22) { index ->
        val y = size.height * (0.63f + index * 0.016f)
        val x = size.width * (0.15f + random.nextFloat() * 0.5f)
        val length = size.width * (0.05f + random.nextFloat() * 0.25f)
        drawLine(
            color = palette.highlight.copy(alpha = 0.13f + random.nextFloat() * 0.1f),
            start = Offset(x, y),
            end = Offset(x + length, y),
            strokeWidth = 1.3f,
            cap = StrokeCap.Round,
        )
    }
    drawSun(Offset(size.width * 0.32f, size.height * 0.2f), size.width * 0.04f, palette.highlight)
}

private fun DrawScope.drawWarmTable(palette: ScenePalette, seed: Int) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(palette.mid.copy(alpha = 0.55f), palette.deep),
            center = Offset(size.width * 0.45f, size.height * 0.4f),
            radius = max(size.width, size.height) * 0.9f,
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.highlight.copy(alpha = 0.22f), Color.Transparent),
            startY = 0f,
            endY = size.height,
        ),
    )

    val random = Random(seed + 71)
    val plateCenter = Offset(size.width * 0.5f, size.height * 0.56f)
    val plateRadius = min(size.width, size.height) * 0.3f
    drawCircle(color = palette.highlight.copy(alpha = 0.88f), radius = plateRadius, center = plateCenter)
    drawCircle(
        color = palette.deep.copy(alpha = 0.13f),
        radius = plateRadius * 0.78f,
        center = plateCenter,
        style = Stroke(width = 3f),
    )
    repeat(7) {
        drawCircle(
            color = palette.mid.copy(alpha = 0.55f + random.nextFloat() * 0.3f),
            radius = plateRadius * (0.07f + random.nextFloat() * 0.12f),
            center = Offset(
                x = plateCenter.x + (random.nextFloat() - 0.5f) * plateRadius * 0.9f,
                y = plateCenter.y + (random.nextFloat() - 0.5f) * plateRadius * 0.9f,
            ),
        )
    }

    val cupCenter = Offset(size.width * 0.18f, size.height * 0.78f)
    drawCircle(color = palette.highlight.copy(alpha = 0.85f), radius = plateRadius * 0.3f, center = cupCenter)
    drawCircle(
        color = palette.deep.copy(alpha = 0.18f),
        radius = plateRadius * 0.22f,
        center = cupCenter,
        style = Stroke(width = 4f),
    )

    val cutleryX = size.width * 0.84f
    drawLine(
        color = palette.highlight.copy(alpha = 0.7f),
        start = Offset(cutleryX, size.height * 0.62f),
        end = Offset(cutleryX, size.height * 0.92f),
        strokeWidth = 6f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = palette.highlight.copy(alpha = 0.5f),
        start = Offset(cutleryX - size.width * 0.06f, size.height * 0.64f),
        end = Offset(cutleryX - size.width * 0.06f, size.height * 0.88f),
        strokeWidth = 4f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSoftPortrait(palette: ScenePalette, seed: Int) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(palette.skyTop, palette.skyBottom),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
    )

    val random = Random(seed + 81)
    repeat(9) {
        val center = Offset(
            x = random.nextFloat() * size.width,
            y = random.nextFloat() * size.height,
        )
        val radius = min(size.width, size.height) * (0.1f + random.nextFloat() * 0.2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.highlight.copy(alpha = 0.22f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }

    val shoulderTop = size.height * 0.7f
    val silhouette = Path().apply {
        moveTo(size.width * 0.16f, size.height)
        cubicTo(
            size.width * 0.22f, shoulderTop,
            size.width * 0.36f, shoulderTop - size.height * 0.02f,
            size.width * 0.4f, shoulderTop - size.height * 0.1f,
        )
        cubicTo(
            size.width * 0.42f, shoulderTop - size.height * 0.17f,
            size.width * 0.58f, shoulderTop - size.height * 0.17f,
            size.width * 0.6f, shoulderTop - size.height * 0.1f,
        )
        cubicTo(
            size.width * 0.64f, shoulderTop - size.height * 0.02f,
            size.width * 0.78f, shoulderTop,
            size.width * 0.84f, size.height,
        )
        close()
    }
    drawPath(silhouette, palette.deep.copy(alpha = 0.82f))

    val headCenter = Offset(size.width * 0.5f, size.height * 0.48f)
    val headRadius = min(size.width, size.height) * 0.14f
    drawCircle(color = palette.deep.copy(alpha = 0.9f), radius = headRadius, center = headCenter)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.highlight.copy(alpha = 0.4f), Color.Transparent),
            center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.45f),
            radius = headRadius * 1.3f,
        ),
        radius = headRadius * 1.3f,
        center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.45f),
    )
}

private fun DrawScope.fillSky(palette: ScenePalette) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.skyTop, palette.skyBottom),
            startY = 0f,
            endY = size.height,
        ),
    )
}

private fun DrawScope.drawSun(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.55f), Color.Transparent),
            center = center,
            radius = radius * 3.4f,
        ),
        radius = radius * 3.4f,
        center = center,
    )
    drawCircle(color = color.copy(alpha = 0.95f), radius = radius, center = center)
}

private fun DrawScope.drawMountainRange(
    baseY: Float,
    amplitude: Float,
    color: Color,
    seed: Int,
) {
    val random = Random(seed)
    val count = 4 + random.nextInt(3)
    val step = size.width / count
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, baseY)
        for (index in 0 until count) {
            val peakX = step * index + step * (0.3f + random.nextFloat() * 0.4f)
            val peakY = baseY - amplitude * (0.45f + random.nextFloat() * 0.55f)
            lineTo(peakX, peakY)
            val valleyX = step * (index + 1)
            val valleyY = baseY - amplitude * (0.02f + random.nextFloat() * 0.16f)
            lineTo(valleyX, valleyY)
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawDune(baseY: Float, color: Color, offset: Float) {
    val path = Path().apply {
        moveTo(0f, baseY)
        cubicTo(
            size.width * 0.25f, baseY - size.height * 0.1f * (1f - offset),
            size.width * 0.55f, baseY - size.height * 0.04f,
            size.width, baseY - size.height * 0.12f * offset,
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawGrain(seed: Int) {
    val random = Random(seed + 404)
    repeat(42) {
        drawCircle(
            color = Color.White.copy(alpha = 0.025f + random.nextFloat() * 0.03f),
            radius = 0.5f + random.nextFloat() * 1.1f,
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height),
        )
    }
}

private fun DrawScope.drawVignette() {
    val radius = max(size.width, size.height) * 0.75f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)),
            center = Offset(size.width * 0.5f, size.height * 0.55f),
            radius = radius,
        ),
    )
}
