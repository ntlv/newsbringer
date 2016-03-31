package se.ntlv.newsbringer.comments

import android.database.Cursor
import android.os.Bundle

class CommentsPresenter
(val commentsViewBinder: CommentsViewBinder,
 val interactor: CommentsInteractor)  {

    fun onHeaderLoaded(postTitle: String, text: String, by: String, time: String, score: String, link: String) {
        commentsViewBinder.indicateDataLoading(false)
        commentsViewBinder.updateHeader(postTitle, text, by, time, score, link)
    }

    fun onCommentsLoaded(data: Cursor?) {
        commentsViewBinder.indicateDataLoading(false)
        commentsViewBinder.updateComments(data)
    }

    fun onViewReady() {
        commentsViewBinder.indicateDataLoading(true)
        interactor.loadData(
                { title: String, text: String, by: String, time: String, score: String, link: String ->
                    onHeaderLoaded(title, text, by, time, score, link)
                },
                { onCommentsLoaded(it) }
        )
    }

    fun refreshData(needToIndicateLoading: Boolean, disallowFetchSkip: Boolean) {
        if (needToIndicateLoading) {
            commentsViewBinder.indicateDataLoading(true)
        }
        interactor.refreshComments(disallowFetchSkip)
    }

    fun onHeaderClick() = interactor.goToLink()

    fun onCommentLongClick(commentId: Long?) = interactor.onCommentLongClick(commentId)

    fun destroy() = interactor.destroy()

    fun onShareStoryClicked() = interactor.shareStory()

    fun onShareCommentsClicked() = interactor.shareComments()

    fun saveTemporaryState(bundle: Bundle) = interactor.saveTemporaryState(bundle)
    fun restoreTemporaryState(savedInstanceState: Bundle?) = interactor.restoreTemporaryState(savedInstanceState)

}