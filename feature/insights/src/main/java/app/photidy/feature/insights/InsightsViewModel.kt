package app.photidy.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.InsightsCalculator
import app.photidy.core.model.InsightsSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class InsightsViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository

    val uiState: StateFlow<InsightsSummary> = combine(
        photoRepository.photos,
        photoRepository.snapshot,
    ) { photos, snapshot ->
        InsightsCalculator.calculate(photos, snapshot.session, System.currentTimeMillis())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsSummary(),
    )
}
