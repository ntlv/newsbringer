package se.ntlv.newsbringer.comments

import se.ntlv.newsbringer.adapter.ObservableData
import se.ntlv.newsbringer.network.CommentUiData

class CommentsPresenter
(val viewBinder: CommentsViewBinder,
 val interactor: CommentsInteractor) {

    fun onHeaderLoaded(postTitle: String, text: String, by: String, time: String, score: String, link: String, descendantsCount: String) {
        viewBinder.updateHeader(postTitle, text, by, time, score, link, descendantsCount)
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
        interactor.loadData(
                { model ->
                    if (model == null) {
                        onHeaderLoaded("", "", "", "", "", "", "")
                    } else {
                        val time = model.time.toString()
                        val score = model.score.toString()
                        val descendantCount = model.descendants.toString()
                        onHeaderLoaded(model.title, model.text, model.by, time, score, model.url, descendantCount)
                    }
                },
                { onCommentsLoaded(it) }
        )
    }

    fun refreshData(needToIndicateLoading: Boolean, allowFetchSkip: Boolean) {
        if (needToIndicateLoading) {
            viewBinder.indicateDataLoading(true)
        }
        interactor.refreshComments(allowFetchSkip)
    }

    fun onHeaderClick() = interactor.goToLink()

    //    fun onCommentLongClick(commentId: Long?) = interactor.onCommentLongClick(commentId)

    fun destroy() = interactor.destroy()

    fun onShareStoryClicked() = interactor.shareStory()

    fun onShareCommentsClicked() = interactor.shareComments()
}