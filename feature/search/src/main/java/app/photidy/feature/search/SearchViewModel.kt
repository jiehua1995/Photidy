package app.photidy.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.PhotoSearch
import app.photidy.core.model.PhotoWithState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val results: List<PhotoWithState> = emptyList(),
)

class SearchViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository
    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = combine(
        photoRepository.photos,
        query,
    ) { photos, currentQuery ->
        SearchUiState(
            isLoading = false,
            query = currentQuery,
            results = PhotoSearch.search(photos, currentQuery),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun updateQuery(value: String) {
        query.value = value
    }

    fun clearQuery() {
        query.value = ""
    }
}
