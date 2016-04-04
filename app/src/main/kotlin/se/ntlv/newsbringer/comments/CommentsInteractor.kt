package se.ntlv.newsbringer.comments

import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.CommentsData
import se.ntlv.newsbringer.adapter.ObservableData
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.database.getStringByName
import se.ntlv.newsbringer.network.CommentUiData
import se.ntlv.newsbringer.network.DataPullPushService
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CommentsInteractor(val context: Context,
                         val loaderManager: LoaderManager,
                         val newsThreadId: Long,
                         val navigator: Navigator) : LoaderManager.LoaderCallbacks<Cursor> {

    var title: String = ""
    var link: String = ""
    val commentsLink: String
        get() {
            return "https://news.ycombinator.com/item?id=$newsThreadId"
        }
    var onHeaderLoadCompletion: ((String, String, String, String, String, String, String) -> Unit) =
            { s: String, s1: String, s2: String, s3: String, s4: String, s5: String, s6 : String -> }

    var onCommentsLoadCompletion: ((ObservableData<CommentUiData>?) -> Unit) = {}

    private val TAG: String = CommentsInteractor::class.java.simpleName

    private val LOADER_ARGS_ID: String = "$TAG:loader_args_id"
    private val handledPositions = HashSet<Long>()
    private val STATE_HANDLED_POSITIONS: String = "${TAG}_handled_positions"

    private var shouldRequestDataIfLoaderFails = AtomicBoolean(true)

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (args == null || args.getLong(LOADER_ARGS_ID, -1L) == -1L) {
            throw IllegalArgumentException("Cannot instantiate loader will null arguments or missing arguments")
        }
        when (id) {
            R.id.loader_comments_comments -> {
                val projection = CommentsTable.getDefaultProjection()
                val selection = "${CommentsTable.COLUMN_PARENT}=?"
                val selectionArgs = arrayOf(args.getLong(LOADER_ARGS_ID).toString())
                val sorting = CommentsTable.COLUMN_ORDINAL + " ASC"
                return CursorLoader(context, NewsContentProvider.CONTENT_URI_COMMENTS,
                        projection, selection, selectionArgs, sorting)
            }
            R.id.loader_comments_header -> {
                val selection = "${PostTable.COLUMN_ID}=?"
                val selectionArgs = arrayOf(args.getLong(LOADER_ARGS_ID).toString())
                return CursorLoader(context, NewsContentProvider.CONTENT_URI_POSTS,
                        PostTable.getCommentsProjection(), selection, selectionArgs, null)
            }
            else -> {
                throw IllegalArgumentException("Invalid loader id")
            }
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        if (loader == null || data == null) {
            return
        }
        when (loader.id) {
            R.id.loader_comments_header -> {
                if (!data.moveToFirst()) {
                    if (shouldRequestDataIfLoaderFails.getAndSet(false)) {
                        refreshComments(true)
                    } else {
                        onLoaderReset(loader)
                    }
                } else {
                    title = data.getStringByName(PostTable.COLUMN_TITLE)
                    link = data.getStringByName(PostTable.COLUMN_URL)
                    val by = data.getStringByName(PostTable.COLUMN_BY)
                    val text = data.getStringByName(PostTable.COLUMN_TEXT)
                    val time = data.getStringByName(PostTable.COLUMN_TIMESTAMP)
                    val score = data.getStringByName(PostTable.COLUMN_SCORE)
                    val descendantCount = data.getStringByName(PostTable.COLUMN_DESCENDANTS)
                    onHeaderLoadCompletion(title, text, by, time, score, link, descendantCount)
                }

            }
            R.id.loader_comments_comments -> {
                if (!data.moveToFirst()) {
                    if (shouldRequestDataIfLoaderFails.getAndSet(false)) {
                        refreshComments(true)
                    } else {
                        onLoaderReset(loader)
                    }
                } else {
                    onCommentsLoadCompletion(CommentsData(data))
                }
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        if (loader == null) {
            throw IllegalArgumentException("Passed null loader in reset")
        }
        when (loader.id) {
            R.id.loader_comments_header -> {
                onHeaderLoadCompletion("", "", "", "", "", "", "")
            }
            R.id.loader_comments_comments -> {
                onCommentsLoadCompletion(null)
            }
        }
    }

    fun loadData(headerCompletion: ((String, String, String, String, String, String, String) -> Unit),
                 commentsCompletion: ((ObservableData<CommentUiData>?) -> Unit)) {
        onHeaderLoadCompletion = headerCompletion
        onCommentsLoadCompletion = commentsCompletion
        val loaderArgs = Bundle()
        loaderArgs.putLong(LOADER_ARGS_ID, newsThreadId)
        loaderManager.initLoader(R.id.loader_comments_comments, loaderArgs, this)
        loaderManager.initLoader(R.id.loader_comments_header, loaderArgs, this)
    }

    fun refreshComments(allowFetchSkip: Boolean) {
        DataPullPushService.startActionFetchComments(context, newsThreadId, allowFetchSkip)
    }

    /*fun onCommentLongClick(commentId: Long?): Boolean {
        if (commentId == null || commentId in handledPositions) {
            Log.d(TAG, "Ignoring click event on view $commentId")
            return false
        }
        handledPositions.add(commentId)
        DataPullPushService.startActionFetchChildComments(context, commentId, newsThreadId)
        return true
    }*/

    fun destroy(): Unit {
        loaderManager.destroyLoader(R.id.loader_comments_comments)
        loaderManager.destroyLoader(R.id.loader_comments_header)
    }

    fun saveTemporaryState(bundle: Bundle) {
        val out = LongArray(handledPositions.size)
        handledPositions.withIndex().forEach { out[it.index] = it.value }
        bundle.putLongArray(STATE_HANDLED_POSITIONS, out)
    }

    fun restoreTemporaryState(savedInstanceState: Bundle?) {
        val positions = savedInstanceState?.getLongArray(STATE_HANDLED_POSITIONS)
        positions?.toCollection(handledPositions)
    }

    fun shareComments() = navigator.goToShareLink(title, commentsLink)

    fun shareStory() = navigator.goToShareLink(title, link)

    fun goToLink() = navigator.goToLink(link)

}