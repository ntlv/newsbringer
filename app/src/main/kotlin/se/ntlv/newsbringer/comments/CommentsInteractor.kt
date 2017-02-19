package se.ntlv.newsbringer.comments

import android.content.Context
import android.support.v7.util.DiffUtil
import rx.Observable
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.customviews.DataDiffCallback
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.database.DataCommentsThread
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.network.IoService
import se.ntlv.newsbringer.network.RowItem

class CommentsInteractor(val context: Context,
                         val database: Database,
                         val newsThreadId: Long,
                         val navigator: Navigator,
                         seed: List<RowItem>?) {

    private var previousComments: List<RowItem>? = seed
    private var shareCommentsInternal: (() -> Unit) = { throw IllegalStateException("Attempting to share comments before initialization") }
    private var shareStoryInternal: (() -> Unit) = { throw IllegalStateException("Attempting to share story before initialization") }
    private var goToLinkInternal: (() -> Unit) = { throw IllegalStateException("Attempting to open link before initialization") }
    private var addToStarredInternal: (() -> Unit) = { throw IllegalStateException("Attempting to toggle starred before initialization") }


    fun loadData(): Observable<AdapterModelCollection<RowItem>> {
        val header = database.getPostById(newsThreadId)
                .mapToOne {
                    val model = RowItem.NewsThreadUiData(it)
                    shareCommentsInternal = { navigator.goToShareLink(model.title, "https://news.ycombinator.com/item?id=$newsThreadId") }
                    shareStoryInternal = { navigator.goToShareLink(model.title, model.url) }
                    goToLinkInternal = { navigator.goToLink(model.url) }
                    addToStarredInternal = { IoService.requestToggleStarred(context, model.id, model.isStarred)}
                    model
                }

        val comments = database.getCommentsForPost(newsThreadId)
                .mapToList {
                    RowItem.CommentUiData(it)
                }

        return Observable.combineLatest(header, comments, { h, c ->
            val new = listOf(h) + c
            val old = previousComments
            previousComments = new

            val diff = DiffUtil.calculateDiff(DataDiffCallback(old, new))

            DataCommentsThread(new, diff)
        })
    }

    fun refreshComments() = IoService.requestFetchPostAndComments(context, newsThreadId)


    fun goToLink() = goToLinkInternal()
    fun shareComments() = shareCommentsInternal()
    fun shareStory() = shareStoryInternal()


    fun addToStarred() = addToStarredInternal()

}
