package se.ntlv.newsbringer.network

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import se.ntlv.newsbringer.application.GlobalDependency
import se.ntlv.newsbringer.thisShouldNeverHappen
import java.io.IOException
import kotlin.LazyThreadSafetyMode.PUBLICATION


object Io {

    const val fetchInc = 15

    //---------------------------------------------------------------------------------------------
    // PUBLIC INTERFACE
    //---------------------------------------------------------------------------------------------
    fun requestFetchComment(id: Long, ancestorCount: Int, baseOrdinal: String, newsThreadId: Long): Unit {
        pool.submit { handleFetchComment(id, ancestorCount, baseOrdinal, newsThreadId) }
    }

    fun requestFetchPostAndComments(id: Long): Unit {
        pool.submit { handleFetchPostAndComments(id) }
    }

    fun requestFetchThreads(range: IntRange): Unit {
        val first = range.first.clamp(0, ORDINAL_MAX_BOUND)
        val last = range.last.clamp(0, ORDINAL_MAX_BOUND)
        (first..last).forEach { ordinal: Int ->
            pool.submit { handleFetchThread(ordinal) }
        }
    }

    fun requestFullWipe(): Unit {
        pool.submit { handleFullWipe() }
    }

    fun requestToggleStarred(id: Long, currentStarredStatus: Int): Unit {
        pool.submit { handleToggleStarred(id, currentStarredStatus) }
    }


    fun requestPrepareHeaderAndCommentsFor(newsThreadId: Long): Unit {
        pool.submit { handlePrepareHeaderAndCommentsFor(newsThreadId) }
    }


    //---------------------------------------------------------------------------------------------
    // IMPLEMENTATION DETAILS
    //---------------------------------------------------------------------------------------------
    private val URI_SUFFIX: String = ".json"
    private val BASE_URI: String = "https://hacker-news.firebaseio.com/v0"
    private val ITEM_URI: String = "$BASE_URI/item/"
    private val TOP_FIVE_HUNDRED: String = "$BASE_URI/topstories$URI_SUFFIX"
    private val ORDINAL_MAX_BOUND = 499

    private val pool by lazy(PUBLICATION) { GlobalDependency.ioPool }
    private val database by lazy(PUBLICATION) { GlobalDependency.database }
    private val http by lazy(PUBLICATION) { GlobalDependency.httpClient }
    private val gson by lazy(PUBLICATION) { GlobalDependency.gson }

    private fun handlePrepareHeaderAndCommentsFor(newsThreadId: Long) {

        val childIds: List<Long>
        val newsItem = database.getPostByIdSync(newsThreadId)
        if (newsItem == null) {
            val fetchedItem = getNewsThread(newsThreadId, 0, 0)
            childIds = fetchedItem.children
            database.insertNewsThreads(fetchedItem)
        } else {
            childIds = newsItem.children
        }

        if (!database.hasCommentsForPostIdSync(newsThreadId)) {
            if (childIds.isEmpty()) {
                val fakeComment = CommentUiData(newsThreadId, 0, "a", System.currentTimeMillis(), 0, "null author", listOf(), "No comments", 0)
                val cv = fakeComment.toContentValues("a", 0, newsThreadId)
                database.insertComment(cv)
            } else {
                var commentOrdinal = "a"
                childIds.forEach {
                    Io.requestFetchComment(it, 0, commentOrdinal, newsThreadId)
                    commentOrdinal = commentOrdinal.nextOrdinal()
                }
            }
        }
    }


    private fun handleFetchComment(id: Long, ancestorCount: Int, baseOrdinal: String, newsThreadId: Long) {
        val url = "$ITEM_URI$id$URI_SUFFIX"

        val comment = http.get(url, CommentUiData::class.java, gson)

        val reified = comment.toContentValues(baseOrdinal, ancestorCount, newsThreadId)
        database.insertComment(reified)

        var childOrdinal = baseOrdinal + 'a'
        val childAncestorCount = ancestorCount.inc()
        comment.kids.forEach {
            Io.requestFetchComment(it, childAncestorCount, childOrdinal, newsThreadId)
            childOrdinal = childOrdinal.nextOrdinal()
        }
    }

    private fun handleFetchPostAndComments(newsThreadId: Long) {
        val post = database.getPostByIdSync(newsThreadId)

        val thread = getNewsThread(newsThreadId, post?.ordinal ?: ORDINAL_MAX_BOUND, post?.isStarred ?: 0)
        database.insertNewsThreads(thread)


        var commentOrdinal = "a"
        thread.children.forEach {
            requestFetchComment(it, 0, commentOrdinal, newsThreadId)
            commentOrdinal = commentOrdinal.nextOrdinal()
        }
    }

    private fun handleToggleStarred(id: Long, currentStarredStatus: Int) {
        val newStatus = (currentStarredStatus + 1) % 2
        database.setStarred(id, newStatus)
    }

    private fun String.nextOrdinal(): String {
        val char = last()
        return when (char) {
            in ('a'..'y') -> "${this.subSequence(0, length - 1)}${char.inc()}"
            'z' -> this + 'a'
            else -> thisShouldNeverHappen("Can't increment character $char from base $this")
        }
    }


    private fun handleFullWipe() {

        val starredBeforeDelete = database.allStarredPosts()
        database.deleteFrontPage()

        val ids = http.get(TOP_FIVE_HUNDRED, Array<Long>::class.java, gson)
        val emptyNewsThreads: List<Pair<Long, Int>> = ids.mapIndexed { ordinal, id -> Pair(id, ordinal) }
        database.insertPlaceholders(emptyNewsThreads)

        val starredItems = starredBeforeDelete.map { getNewsThread(it.id, it.ordinal, 1) }.toTypedArray()
        database.insertNewsThreads(*starredItems)

        requestFetchThreads(0..fetchInc)
    }

    private fun handleFetchThread(ordinal: Int) {
        val localEntity = database.getPostByOrdinalSync(ordinal)

        val id = localEntity?.id ?: http.get(TOP_FIVE_HUNDRED, Array<Long>::class.java, gson)[ordinal]
        val isStarred = localEntity?.isStarred ?: 0

        val networkEntity = getNewsThread(id, ordinal, isStarred)

        database.insertNewsThreads(networkEntity)
    }

    private fun getNewsThread(id: Long, ordinal: Int, isStarred: Int = 0): NewsThreadUiData {
        val item = http.get("$ITEM_URI$id$URI_SUFFIX", NewsThreadUiData::class.java, gson)
        item.ordinal = ordinal
        item.isStarred = isStarred
        return item
    }


    private fun <T> OkHttpClient.get(url: String, cls: Class<T>, deserializer: Gson): T {
        val request = Request.Builder().url(url).get().build()
        newCall(request).execute().use {
            if (!it.isSuccessful) throw IOException("Unexpected response { $it }")

            it.body().use {
                it.charStream().use {
                    return deserializer.fromJson(it, cls)
                }
            }

        }
    }

}

private fun Int.clamp(lower: Int, upper: Int): Int = when {
    this < lower -> lower
    this > upper -> upper
    else -> this
}
