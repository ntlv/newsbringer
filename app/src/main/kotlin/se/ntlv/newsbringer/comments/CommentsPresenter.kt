package se.ntlv.newsbringer.comments

import se.ntlv.newsbringer.adapter.ObservableData
import se.ntlv.newsbringer.network.CommentUiData
import se.ntlv.newsbringer.network.NewsThreadUiData

class CommentsPresenter
(val viewBinder: CommentsViewBinder,
 val interactor: CommentsInteractor) {

    fun onHeaderLoaded(model: NewsThreadUiData?) {
        val time = model?.time.toString()
        val score = model?.score.toString()
        val descendantCount = model?.descendants.toString()
        val title = model?.title ?: ""
        val text = model?.text ?: ""
        val by = model?.by ?: ""
        val url = model?.url ?: ""
        viewBinder.updateHeader(title, text, by, time, score, url, descendantCount)
        viewBinder.indicateDataLoading(false)
    }

    fun onCommentsLoaded(data: ObservableData<CommentUiData>?) {
        viewBinder.updateComments(data)
        if (data?.hasContent() ?: false) {
            viewBinder.indicateDataLoading(false)
        }
    }

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        interactor.attach({ onHeaderLoaded(it) }, { onCommentsLoaded(it) })
        interactor.loadData()
    }

    fun refreshData(needToIndicateLoading: Boolean, allowFetchSkip: Boolean) {
        if (needToIndicateLoading) {
            viewBinder.indicateDataLoading(true)
        }
        interactor.refreshComments(allowFetchSkip)
    }

    fun onHeaderClick() = interactor.goToLink()

    fun destroy() = interactor.destroy()

    fun onShareStoryClicked() = interactor.shareStory()

    fun onShareCommentsClicked() = interactor.shareComments()
}