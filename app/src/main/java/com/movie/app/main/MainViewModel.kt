package com.movie.app.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.api.result.MoviesResult
import com.movie.app.modules.Movie
import com.movie.app.modules.MovieSearchFilter
import com.movie.app.modules.MovieSortType
import com.movie.app.repositories.MovieRepository
import com.movie.app.util.Result
import com.movie.app.util.schedulers.BaseExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val executor: BaseExecutor,
    private val movieRepo: MovieRepository,
    private val searchFilter: MovieSearchFilter
) : ViewModel() {

    private var _favStatus: MutableLiveData<HashSet<Long>> = MutableLiveData()
    private var _result: MutableLiveData<Result<MoviesResult>> = MutableLiveData()

    val result: LiveData<Result<MoviesResult>> = _result
    val favStatuses: LiveData<HashSet<Long>> = _favStatus

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        searchFilter.pageNumber = MovieSearchFilter.First_PAGE
        loadData()
    }

    fun loadNextPage() {
        loadData()
    }

    private fun loadData() {
        val isFirstPage = searchFilter.isFirstPage()
        if (isFirstPage) {
            _result.postValue(Result.loading())
        }
        viewModelScope.launch(executor.io()) {
            try {
                if (searchFilter.isFirstPage()) {
                    movieRepo.getLocalMovies(searchFilter)?.let {
                        _result.postValue(Result.success(it))
                    }
                }
                movieRepo.getRemoteMovies(searchFilter)?.let {
                    _result.postValue(Result.success(it))
                }
                searchFilter.incrementPage()
            } catch (e: Exception) {
                _result.postValue(Result.failure(e))
            }
        }
    }

    fun onSearchFilterChanged(movieSortType: MovieSortType) {
        searchFilter.sortBy = movieSortType
        searchFilter.pageNumber = MovieSearchFilter.First_PAGE
        loadData()
    }

    fun addRemoveFavMovie(movie: Movie) {
        viewModelScope.launch(Dispatchers.IO) {
            movieRepo.removeAddFavMovie(movie.id, movie.isFav)
        }
    }

    fun syncFavouritesStatues() {
        viewModelScope.launch(executor.io()) {
            val ids = movieRepo.getFavMovieIds()
            _favStatus.postValue(ids)
        }
    }
}
