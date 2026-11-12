package com.mymovie.log.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Kept in SavedStateHandle so the query survives fold/unfold, rotation and process recreation
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    val isSearchActive: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_ACTIVE, false)

    val searchResults = query
        .debounce(400)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(PagingData.empty<Movie>())
            else searchMoviesUseCase(query)
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(newQuery: String) {
        savedStateHandle[KEY_QUERY] = newQuery
    }

    fun onActiveChange(active: Boolean) {
        savedStateHandle[KEY_ACTIVE] = active
    }

    fun clearSearch() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_ACTIVE] = false
    }

    private companion object {
        const val KEY_QUERY = "search_query"
        const val KEY_ACTIVE = "search_active"
    }
}
