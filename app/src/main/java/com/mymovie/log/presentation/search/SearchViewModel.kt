package com.mymovie.log.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.usecase.SearchMoviesUseCase
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val movies: List<Movie>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<SearchUiState> = _query
        .debounce(400)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                AppLogger.d("VM_SEARCH", "Query cleared → Idle")
                kotlinx.coroutines.flow.flowOf(SearchUiState.Idle)
            } else {
                AppLogger.d("VM_SEARCH", "Searching: queryLength=${query.length}")
                searchMoviesUseCase(query)
                    .map<List<Movie>, SearchUiState> { movies ->
                        AppLogger.d("VM_SEARCH", "Search result: count=${movies.size}")
                        if (movies.isEmpty()) SearchUiState.Success(emptyList())
                        else SearchUiState.Success(movies)
                    }
                    .catch { e ->
                        AppLogger.e("VM_SEARCH", "Search failed: ${e.message}", e)
                        emit(SearchUiState.Error(e.message ?: "검색 실패"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Idle)

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }
}
