package app.photidy.core.model

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthCount(
    val year: Int,
    val month: Int,
    val count: Int,
)

data class SessionStats(
    val total: Int,
    val reviewed: Int,
    val markedForDeletion: Int,
    val favorites: Int,
)

data class InsightsSummary(
    val totalPhotos: Int = 0,
    val organizedCount: Int = 0,
    val deleteCandidateCount: Int = 0,
    val favoriteCount: Int = 0,
    val monthly: List<MonthCount> = emptyList(),
    val session: SessionStats? = null,
    val reclaimBytes: Long = 0L,
) {
    val organizedRatio: Float
        get() = if (totalPhotos == 0) 0f else organizedCount.toFloat() / totalPhotos
}

object InsightsCalculator {

    fun calculate(
        photos: List<PhotoWithState>,
        session: OrganizeSession?,
        now: Long,
    ): InsightsSummary {
        val zone = ZoneId.systemDefault()
        val deleteItems = photos.filter { it.state.decision == PhotoDecision.DELETE_CANDIDATE }

        val currentMonth = YearMonth.from(Instant.ofEpochMilli(now).atZone(zone))
        val monthly = (11 downTo 0).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            val count = photos.count { item ->
                val captured = Instant.ofEpochMilli(item.photo.capturedAt).atZone(zone)
                captured.year == month.year && captured.monthValue == month.monthValue
            }
            MonthCount(month.year, month.monthValue, count)
        }

        val sessionStats = session?.let { active ->
            val byId = photos.associateBy { it.photo.id }
            val sessionPhotos = active.queue.mapNotNull { byId[it] }
            SessionStats(
                total = active.total,
                reviewed = sessionPhotos.count { it.state.isReviewed },
                markedForDeletion = sessionPhotos.count {
                    it.state.decision == PhotoDecision.DELETE_CANDIDATE
                },
                favorites = sessionPhotos.count { it.state.favorite },
            )
        }

        return InsightsSummary(
            totalPhotos = photos.size,
            organizedCount = photos.count { it.state.isReviewed },
            deleteCandidateCount = deleteItems.size,
            favoriteCount = photos.count { it.state.favorite },
            monthly = monthly,
            session = sessionStats,
            reclaimBytes = deleteItems.sumOf { it.photo.sizeBytes },
        )
    }
}
