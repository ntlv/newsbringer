package se.ntlv.newsbringer.comments

import android.app.LoaderManager
import android.content.ContentResolver
import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.os.CancellationSignal
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.ObservableCursorData
import se.ntlv.newsbringer.adapter.ObservableData
import se.ntlv.newsbringer.database.*
import se.ntlv.newsbringer.network.CommentUiData
import se.ntlv.newsbringer.network.DataPullPushService
import se.ntlv.newsbringer.network.NewsThreadUiData
import java.util.concurrent.atomic.AtomicBoolean

class CommentsInteractor(val context: Context,
                         val loaderManager: LoaderManager,
                         val newsThreadId: Long,
                         val navigator: Navigator) {

    inner class HeaderCallbacks : LoaderManager.LoaderCallbacks<TypedCursor<NewsThreadUiData>> {
        override fun onLoadFinished(loader: Loader<TypedCursor<NewsThreadUiData>>?, data: TypedCursor<NewsThreadUiData>?) {
            if (data?.hasContent?.not() ?: false) {
                if (shouldRequestDataIfLoaderFails.getAndSet(false)) {
                    refreshComments(true)
                } else {
                    onLoaderReset(loader)
                }
            } else {
                val model = data!!.getRow(0)
                title = model.title
                link = model.url

                onHeaderLoadCompletion(model)
            }
        }

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<TypedCursor<NewsThreadUiData>>? {
            if (args == null || args.getLong(LOADER_ARGS_ID, -1L) == -1L) {
                throw IllegalArgumentException("Cannot instantiate loader will null arguments or missing arguments")
            }
            val threadId = args.getLong(LOADER_ARGS_ID)
            return NewsThreadHeaderLoader(context, threadId)
        }

        override fun onLoaderReset(loader: Loader<TypedCursor<NewsThreadUiData>>?) {
            onHeaderLoadCompletion(null)
        }

    }

    inner class CommentsCallbacks : LoaderManager.LoaderCallbacks<TypedCursor<CommentUiData>> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<TypedCursor<CommentUiData>>? {
            if (args == null || args.getLong(LOADER_ARGS_ID, -1L) == -1L) {
                throw IllegalArgumentException("Cannot instantiate loader will null arguments or missing arguments")
            }
            val threadId = args.getLong(LOADER_ARGS_ID)
            return NewsThreadCommentsLoader(context, threadId)
        }

        override fun onLoadFinished(loader: Loader<TypedCursor<CommentUiData>>?, data: TypedCursor<CommentUiData>?) {
            if (data?.hasContent?.not() ?: false) {
                if (shouldRequestDataIfLoaderFails.getAndSet(false)) {
                    refreshComments(true)
                } else {
                    onLoaderReset(loader)
                }
            } else {
                onCommentsLoadCompletion(ObservableCursorData(data!!))
            }
        }

        override fun onLoaderReset(loader: Loader<TypedCursor<CommentUiData>>?) {
            onCommentsLoadCompletion(null)
        }
    }

    var title: String = ""
    var link: String = ""
    val commentsLink: String
        get() {
            return "https://news.ycombinator.com/item?id=$newsThreadId"
        }
    var onHeaderLoadCompletion: ((NewsThreadUiData?) -> Unit) = { model -> }

    var onCommentsLoadCompletion: ((ObservableData<CommentUiData>?) -> Unit) = {}

    private val TAG: String = CommentsInteractor::class.java.simpleName

    private val LOADER_ARGS_ID: String = "$TAG:loader_args_id"

    private var shouldRequestDataIfLoaderFails = AtomicBoolean(true)

    fun loadData(headerCompletion: ((NewsThreadUiData?) -> Unit),
                 commentsCompletion: ((ObservableData<CommentUiData>?) -> Unit)) {
        onHeaderLoadCompletion = headerCompletion
        onCommentsLoadCompletion = commentsCompletion
        val loaderArgs = Bundle()
        loaderArgs.putLong(LOADER_ARGS_ID, newsThreadId)
        loaderManager.initLoader(R.id.loader_comments_comments, loaderArgs, CommentsCallbacks())
        loaderManager.initLoader(R.id.loader_comments_header, loaderArgs, HeaderCallbacks())
    }

    fun refreshComments(allowFetchSkip: Boolean) {
        DataPullPushService.startActionFetchComments(context, newsThreadId, allowFetchSkip)
    }

    fun destroy(): Unit {
        loaderManager.destroyLoader(R.id.loader_comments_comments)
        loaderManager.destroyLoader(R.id.loader_comments_header)
    }

    fun shareComments() = navigator.goToShareLink(title, commentsLink)

    fun shareStory() = navigator.goToShareLink(title, link)

    fun goToLink() = navigator.goToLink(link)
}

class NewsThreadHeaderLoader(context: Context, val threadId: Long) : TypedCursorLoader<NewsThreadUiData>(context) {
    override fun query(resolver: ContentResolver, signal: CancellationSignal?): TypedCursor<NewsThreadUiData> {
        val query = NewsContentProvider.threadHeaderQuery(threadId)
        val rawCursor = resolver.query(
                query.url,
                query.projection,
                query.selection,
                query.selectionArgs,
                query.sorting,
                signal
        )
        return PostTable.PostTableCursor(rawCursor)
    }
}

class NewsThreadCommentsLoader(context: Context, val threadId: Long) : TypedCursorLoader<CommentUiData>(context) {
    override fun query(resolver: ContentResolver, signal: CancellationSignal?): TypedCursor<CommentUiData> {
        val query = NewsContentProvider.threadCommentsQuery(threadId)
        val rawCursor = resolver.query(
                query.url,
                query.projection,
                query.selection,
                query.selectionArgs,
                query.sorting,
                signal
        )
        return CommentsTable.CommentsTableCursor(rawCursor)
    }

}
