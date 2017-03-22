package se.ntlv.newsbringer.comments

import android.support.v7.util.DiffUtil
import rx.Observable
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.customviews.DataDiffCallback
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.database.DataCommentsThread
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.database.ParcelableIdentifiable
import se.ntlv.newsbringer.network.CommentUiData
import se.ntlv.newsbringer.network.Io
import se.ntlv.newsbringer.network.NewsThreadUiData
import se.ntlv.newsbringer.network.RowItem

class CommentsInteractor(
        val database: Database,
        val newsThreadId: Long,
        val navigator: Navigator,
        seed: List<ParcelableIdentifiable>?) {

    private var previousComments: List<ParcelableIdentifiable>? = seed
    private lateinit var shareCommentsInternal : () -> Boolean
    private lateinit var shareStoryInternal : () -> Boolean
    private lateinit var goToLinkInternal : () -> Boolean
    private lateinit var addToStarredInternal : () -> Unit

    fun loadData(): Observable<AdapterModelCollection<RowItem>> {
        Io.requestPrepareHeaderAndCommentsFor(newsThreadId)
        val header = database.getPostById(newsThreadId)
                .mapToOne {
                    val model = NewsThreadUiData(it)
                    shareCommentsInternal = { navigator.goToShareLink(model.title, "https://news.ycombinator.com/item?id=$newsThreadId") }
                    shareStoryInternal = { navigator.goToShareLink(model.title, model.url) }
                    goToLinkInternal = { navigator.goToLink(model.url) }
                    addToStarredInternal = { Io.requestToggleStarred(model.id, model.isStarred) }
                    model
                }

        val comments = database.getCommentsForPost(newsThreadId)
                .mapToList(::CommentUiData)
                .filter { it.isNotEmpty() }

        return Observable.combineLatest(header, comments) { h, c ->
            val new = listOf(h) + c
            val old = previousComments
            previousComments = new

            val diff = DiffUtil.calculateDiff(DataDiffCallback(old, new))

            DataCommentsThread(new, diff)
        }
    }

    fun refreshComments() = Io.requestFetchPostAndComments(newsThreadId)

    fun goToLink() = goToLinkInternal()
    fun shareComments() = shareCommentsInternal()
    fun shareStory() = shareStoryInternal()

    fun addToStarred() = addToStarredInternal()

}
