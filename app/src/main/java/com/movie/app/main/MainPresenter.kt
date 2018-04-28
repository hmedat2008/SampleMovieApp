package com.movie.app.main

import com.movie.app.api.result.LatestMoviesResult
import com.movie.app.interactors.IMoviesInteractor
import com.movie.app.modules.MovieSearchFilter
import com.movie.app.util.schedulers.BaseSchedulerProvider
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class MainPresenter @Inject constructor(private val schedulerProvider: BaseSchedulerProvider
                                        , private val moviesInteractor: IMoviesInteractor
                                        , private val view: MainActivityContractor.View
                                        , private val searchFilter: MovieSearchFilter)
    : MainActivityContractor.Presenter {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun subscribe() {
        loadFirstPage()
    }

    override fun loadFirstPage() {
        searchFilter.pageNumber = MovieSearchFilter.First_PAGE
        loadData()
    }

    override fun loadNextPage() {
        loadData()
    }

    private fun loadData() {
        val isFirstPage = searchFilter.isFirstPage()
        if (isFirstPage) {
            view.showProgressBar()
        }
        moviesInteractor.getLatest(searchFilter)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(object : Observer<LatestMoviesResult> {
                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(result: LatestMoviesResult) {
                        if (!result.isLoadMore() && (result.results == null
                                        || result.results?.isEmpty()!!)) {
                            view.showNoData()
                        }
                        view.showData(result)
                        searchFilter.pageNumber = searchFilter.pageNumber + 1
                        view.hideProgressBar()
                    }

                    override fun onError(throwable: Throwable) {
                        view.hideProgressBar()
                        view.showError(isFirstPage, throwable)
                    }

                    override fun onComplete() {

                    }
                })
    }


    override fun unSubscribe() {
        compositeDisposable.clear()
    }

}