package se.ntlv.newsbringer.network

import android.app.IntentService
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import okhttp3.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.bundleOf
import se.ntlv.newsbringer.database.*
import se.ntlv.newsbringer.database.NewsContentProvider.Companion.CONTENT_URI_COMMENTS
import se.ntlv.newsbringer.database.NewsContentProvider.Companion.CONTENT_URI_POSTS
import java.io.File
import java.io.IOException
import java.io.Reader

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
class AsyncDataService : IntentService(AsyncDataService::class.java.simpleName), AnkoLogger {

    enum class Action(private val synchronousExecute: (AsyncDataService, Bundle) -> Unit) : AnkoLogger {
        FETCH_COMMENTS({ service, args -> service.handleFetchComments(args.getLong(Extra.NEWS_THREAD_ID.name)) }),
        TOGGLE_STARRED({ service, args -> service.toggleStarred(args.getLong(Extra.NEWS_THREAD_ID.name)) }),
        FETCH_THREADS({ service, args -> service.handleFetchThreads(args.getInt(Extra.CURRENT_MAX.name), args.getBoolean(Extra.DO_FULL_WIPE.name)) }) {
            override fun asyncExecute(context: Context, id: Long, currentMax: Int, doFullWipe: Boolean): Boolean {
                if (currentMax >= 500) {
                    return false
                }
                return asyncExecute(context, this, Extra.CURRENT_MAX to currentMax, Extra.DO_FULL_WIPE to doFullWipe)
            }
        };

        protected enum class Extra {
            NEWS_THREAD_ID, CURRENT_MAX, DO_FULL_WIPE;
        }

        @JvmOverloads
        open fun asyncExecute(context: Context, id: Long = -1, currentMax: Int = 0, doFullWipe: Boolean = false): Boolean {
            return asyncExecute(context, this, Extra.NEWS_THREAD_ID to id)
        }


        protected fun asyncExecute(context: Context, action: Action, vararg args: Pair<Extra, Any>): Boolean {
            val command = Intent(action.name, null, context, AsyncDataService::class.java)
            val processedArgs = args.map { Pair(it.first.name, it.second) }.toTypedArray()
            command.putExtras(bundleOf(*processedArgs))
            context.startService(command)!!
            return true
        }

        operator fun invoke(s: AsyncDataService, a: Bundle) {
            synchronousExecute(s, a)
        }
    }

    private val URI_SUFFIX: String = ".json"
    private val BASE_URI: String = "https://hacker-news.firebaseio.com/v0"
    private val ITEM_URI: String = "$BASE_URI/item/"
    private val TOP_FIVE_HUNDRED: String = "$BASE_URI/topstories$URI_SUFFIX"

    private val resolver: ContentResolver by lazy { contentResolver }

    private val gson: Gson by lazy { Gson() }

    private val okHttp: OkHttpClient by lazy {
        val httpCacheDirectory: File = File(cacheDir, "responses")
        val cacheSize: Long = 10 * 1024 * 1024 // 10 MiB
        OkHttpClient.Builder()
                .cache(Cache(httpCacheDirectory, cacheSize))
                .build()
    }

    override fun onHandleIntent(intent: Intent) = Action.valueOf(intent.action)(this, intent.extras)

    private fun toggleStarred(id: Long) {
        val projection = arrayOf(PostTable.COLUMN_ID, PostTable.COLUMN_STARRED)
        val starred = resolver.getRowById(CONTENT_URI_POSTS, projection, id, { it.getIntByName(PostTable.COLUMN_STARRED) })

        val newStatus = starred.plus(1).mod(2)


        val cv = contentValuesOf(PostTable.COLUMN_STARRED to newStatus)
        resolver.updateRowById(CONTENT_URI_POSTS, id, cv)
    }

    private fun handleFetchComments(newsThreadId: Long) {
        // valid id?
        if (newsThreadId < 1L) {
            throw IllegalArgumentException("Thread id can't be -1")
        }
        val projection = PostTable.getOrdinalAndStarredProjection()
        val ordinalSelection = "${PostTable.COLUMN_ID}=$newsThreadId"

        val ordinalCursor = resolver.query(CONTENT_URI_POSTS, projection, ordinalSelection)
        ordinalCursor.moveToPositionOrThrow(0)

        val ordinal = ordinalCursor.getIntByName(PostTable.COLUMN_ORDINAL)
        val isStarred = ordinalCursor.getIntByName(PostTable.COLUMN_STARRED)
        ordinalCursor.close()

        val thread = blockingGetRequestToModel("$ITEM_URI$newsThreadId$URI_SUFFIX", NewsThread::class.java)
        resolver.insert(CONTENT_URI_POSTS, thread.toContentValues(ordinal, isStarred == 1))


        val work: MutableList<Pair<Long, Int>> = thread.kids?.map { it.to(0) }?.toMutableList() ?: mutableListOf()

        var commentOrdinal = 0

        while (work.isNotEmpty()) {
            val task = work.removeAt(0)
            val url = "$ITEM_URI${task.first}$URI_SUFFIX"

            val comment = blockingGetRequestToModel(url, Comment::class.java)

            val reified = comment.toContentValues(commentOrdinal, task.second, newsThreadId)
            resolver.insert(CONTENT_URI_COMMENTS, reified)

            //inc one for the inserted comment and then one each for every child comment
            commentOrdinal += 1 + comment.kids.size

            //push all direct children to work stack
            val newAncestorCount = task.second + 1
            comment.kids.forEach {
                work.add(0, it.to(newAncestorCount))
            }
        }
    }


    private fun handleFetchThreads(currentMax: Int, doFullWipe: Boolean) {
        if ((currentMax !in 0..490) and !doFullWipe) {
            throw IllegalArgumentException("Invalid range of current max")
        }
        if (doFullWipe) {
            val starredProjection = PostTable.getOrdinalAndStarredProjection()
            val sel = PostTable.STARRED_SELECTION
            val starredSelArgs = arrayOf(PostTable.STARRED_SELECTION_ARGS)

            val starredThreads = resolver.query(CONTENT_URI_POSTS, starredProjection, sel, starredSelArgs)
            val starredAndOrdinalIds = starredThreads.toList({
                it.getLongByName(PostTable.COLUMN_ID) to it.getIntByName(PostTable.COLUMN_ORDINAL)
            })
            val starredIds = starredAndOrdinalIds.map { it.first }
            starredThreads.close()

            resolver.delete(CONTENT_URI_POSTS, null, null)

            val ids = blockingGetRequestToModel(TOP_FIVE_HUNDRED, Array<Long>::class.java)
            val emptyNewsThreads = ids.withIndex()
                    .filter { it.value !in starredIds }
                    .map { NewsThread(it.value).toContentValues(it.index) }

            val starredItems = starredAndOrdinalIds.map {
                val itemUrl = "$ITEM_URI${it.first}$URI_SUFFIX"
                val item = blockingGetRequestToModel(itemUrl, NewsThread::class.java)
                val ordinal = it.second
                item.toContentValues(ordinal, true)
            }

            val compound = (starredItems + emptyNewsThreads).toTypedArray()
            resolver.bulkInsert(CONTENT_URI_POSTS, compound)
        }

        val proj = PostTable.getFrontPageProjection()
        val sel = "(${PostTable.COLUMN_TITLE} is null or ${PostTable.COLUMN_TITLE} = '') and ${PostTable.COLUMN_STARRED} = 0"
        val orderBy = " ${PostTable.COLUMN_ORDINAL} ASC "
        val emptyThreadsCursor = resolver.query(NewsContentProvider.CONTENT_URI_POSTS_W_LIMIT(10), proj, sel, null, orderBy)
        check(emptyThreadsCursor.count <= 10)
        emptyThreadsCursor.toList {
            it.getLongByName(PostTable.COLUMN_ID) to it.getIntByName(PostTable.COLUMN_ORDINAL)
        }.forEach {
            val item = blockingGetRequestToModel("$ITEM_URI${it.first}$URI_SUFFIX", NewsThread::class.java)
            val cv = item.toContentValues(it.second)
            resolver.insert(CONTENT_URI_POSTS, cv)
        }
        emptyThreadsCursor.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        okHttp.cache().close()
    }

    fun <T> blockingGetRequestToModel(url: String, cls: Class<T>): T {
        var reader: Reader? = null
        var body: ResponseBody? = null
        var response: Response? = null
        try {
            val request = Request.Builder().url(url).get().build()
            response = okHttp.newCall(request).execute()
            if (response.isSuccessful.not()) throw IOException("Unexpected response { $response }")
            body = response.body()
            reader = body.charStream()
            return gson.fromJson<T>(reader, cls)
        } finally {
            response?.close()
            reader?.close()
            body?.close()
        }
    }

}

