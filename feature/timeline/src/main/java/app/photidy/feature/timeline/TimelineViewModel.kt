package app.photidy.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.Timeline
import app.photidy.core.model.TimelineSection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TimelineUiState(
    val isLoading: Boolean = true,
    val sections: List<TimelineSection> = emptyList(),
    val totalPhotos: Int = 0,
)

class TimelineViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository

    val uiState: StateFlow<TimelineUiState> = photoRepository.photos
        .map { photos ->
            val sections = Timeline.sections(photos)
            TimelineUiState(
                isLoading = false,
                sections = sections,
                totalPhotos = photos.size,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimelineUiState(),
        )
}
