package se.ntlv.newsbringer.comments

import android.app.LoaderManager
import android.content.ContentResolver
import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.os.CancellationSignal
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.bundleOf
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.GenericRecyclerViewAdapter
import se.ntlv.newsbringer.database.*
import se.ntlv.newsbringer.network.AsyncDataService
import se.ntlv.newsbringer.network.RowItem

class CommentsInteractor(val context: Context,
                         val loaderManager: LoaderManager,
                         val newsThreadId: Long,
                         val navigator: Navigator) : LoaderManager.LoaderCallbacks<TypedCursor<RowItem>> {

    private val LOADER_ARGS_ID: String = "${CommentsInteractor::class.java.simpleName}:loader_args_id"

    private lateinit var onContentLoadCompletion: ((TypedCursor<RowItem>?) -> Unit)
    private var shareCommentsInternal: (() -> Unit) = { throw IllegalStateException("Attempting to share comments before initialization") }
    private var shareStoryInternal: (() -> Unit) = { throw IllegalStateException("Attempting to share story before initialization") }
    private var goToLinkInternal: (() -> Unit) = { throw IllegalStateException("Attempting to open link before initialization") }

    override fun onLoaderReset(p0: Loader<TypedCursor<RowItem>>?) {
        onContentLoadCompletion(null)
    }

    override fun onLoadFinished(p0: Loader<TypedCursor<RowItem>>?,
                                data: TypedCursor<RowItem>?) {

        val model = data!![0] as RowItem.NewsThreadUiData

        shareCommentsInternal = { navigator.goToShareLink(model.title, "https://news.ycombinator.com/item?id=$newsThreadId") }
        shareStoryInternal = { navigator.goToShareLink(model.title, model.url) }
        goToLinkInternal = { navigator.goToLink(model.url) }

        onContentLoadCompletion(data)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<TypedCursor<RowItem>> {
        if (args.getLong(LOADER_ARGS_ID, -1L) == -1L) {
            throw IllegalArgumentException("Cannot instantiate loader will null arguments or missing arguments")
        }
        val threadId = args.getLong(LOADER_ARGS_ID)

        val f = { previous : TypedCursor<RowItem>?, resolver: ContentResolver, signal: CancellationSignal ->
            val headerQuery = NewsContentProvider.threadHeaderQuery(threadId)
            val headerRawCursor = resolver.query(
                    headerQuery.url,
                    headerQuery.projection,
                    headerQuery.selection,
                    headerQuery.selectionArgs,
                    headerQuery.sorting,
                    signal
            )
            val headerCursor = PostTable.PostTableCursor(headerRawCursor)

            val header: RowItem.NewsThreadUiData = headerCursor[0]
            headerCursor.close()


            val commentsQuery = NewsContentProvider.threadCommentsQuery(threadId)
            val commentsRawCursor = resolver.query(
                    commentsQuery.url,
                    commentsQuery.projection,
                    commentsQuery.selection,
                    commentsQuery.selectionArgs,
                    commentsQuery.sorting,
                    signal
            )
            val commentsCursor = CommentsTable.CommentsTableCursor(commentsRawCursor)
            val retVal = CompositeCursor(header, commentsCursor)
            val diff = DiffUtil.calculateDiff(GenericRecyclerViewAdapter.DataDiffCallback(previous, retVal))
            retVal.diff = diff
            retVal
        }
        return TypedCursorLoader(context, f)
    }

    fun loadData() {
        val loaderArgs = bundleOf(Pair(LOADER_ARGS_ID, newsThreadId))
        loaderManager.initLoader(R.id.loader_combined_comments_header, loaderArgs, this)
    }

    fun refreshComments() = AsyncDataService.Action.FETCH_COMMENTS.asyncExecute(context, id = newsThreadId)


    fun goToLink() = goToLinkInternal()
    fun shareComments() = shareCommentsInternal()
    fun shareStory() = shareStoryInternal()

    fun attach(onContentLoaded: (TypedCursor<RowItem>?) -> Unit) {
        onContentLoadCompletion = onContentLoaded
    }

    fun addToStarred(): Unit {
        AsyncDataService.Action.TOGGLE_STARRED.asyncExecute(context, newsThreadId)
    }
}
