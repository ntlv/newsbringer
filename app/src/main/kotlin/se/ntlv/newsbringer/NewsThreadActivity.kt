package se.ntlv.newsbringer

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.app.LoaderManager
import android.database.Cursor
import android.content.Loader
import se.ntlv.newsbringer.network.Metadata
import android.widget.TextView
import android.view.View
import android.widget.ResourceCursorAdapter
import kotlin.properties.Delegates
import android.widget.ListView
import android.content.CursorLoader
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.network.DataPullPushService
import android.util.Log


public class NewsThreadActivity : Activity(), LoaderManager.LoaderCallbacks<Cursor> {
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor>? {
        val selection = "${CommentsTable.COLUMN_PARENT}=$mId"
        return CursorLoader(this, NewsContentProvider.CONTENT_URI_COMMENTS, PROJECTION, selection, null, CommentsTable.COLUMN_ORDINAL + "ASC")
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor?) {
        mAdapter.swapCursor(cursor)
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        mAdapter.swapCursor(null)
    }

    private val mAdapter: CommentsListAdapter by Delegates.lazy {
        CommentsListAdapter(this, R.layout.list_item_comment, null, 0)
    }

    var mId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super< Activity>.onCreate(savedInstanceState)


        val args = getIntent().getExtras()

        mId = args.getLong(EXTRA_NEWSTHREAD_ID)
        if (mId != null) {
            Log.d(TAG, "Attempting to fetch comments for $mId")
            DataPullPushService.startActionFetchComments(this, mId)
        }
        val title = args.getString(EXTRA_NEWSTHREAD_TITLE)
        val text = args.getString(EXTRA_NEWSTHREAD_TEXT)
        val by = args.getString(EXTRA_NEWSTHREAD_BY)
        val time = args.getString(EXTRA_NEWSTHREAD_TIME)
        val score = args.getString(EXTRA_NEWSTHREAD_SCORE)
        val link = args.getString(EXTRA_NEWSTHREAD_LINK)

        val array = array(
                Pair(R.id.title, title),
                Pair(R.id.text, text),
                Pair(R.id.by, by),
                Pair(R.id.time, time),
                Pair(R.id.score, score),
                Pair(R.id.link, link)
        )

        setContentView(R.layout.activity_newsthread)
        array.forEach { findViewAndSetText(it.first, it.second) }

        val listView = findViewById(R.id.comment_list) as ListView
        listView.setAdapter(mAdapter)
        getLoaderManager().initLoader(0, null, this)
    }

    fun findViewAndSetText(id: Int, text: String) {
        val view = findViewById(id)
        if (view is TextView) view.setText(text)
    }

    class object {
        val TAG: String = javaClass<NewsThreadActivity>().getSimpleName()

        val EXTRA_NEWSTHREAD_TITLE: String = "${TAG}extra_news_thread"
        val EXTRA_NEWSTHREAD_ID: String = "${TAG}extra_news_thread_id"
        val EXTRA_NEWSTHREAD_TEXT: String = "${TAG}extra_news_thread_text"
        val EXTRA_NEWSTHREAD_BY: String = "${TAG}extra_news_thread_by"
        val EXTRA_NEWSTHREAD_TIME: String = "${TAG}extra_news_thread_time"
        val EXTRA_NEWSTHREAD_SCORE: String = "${TAG}extra_news_thread_score"
        val EXTRA_NEWSTHREAD_LINK: String = "${TAG}extra_news_thread_link"

        private val PROJECTION = array(
                CommentsTable.COLUMN_BY,
                CommentsTable.COLUMN_TEXT,
                CommentsTable.COLUMN_TIME,
                CommentsTable.COLUMN_ORDINAL,
                CommentsTable.COLUMN_ID
        )

        public fun getIntent(ctx: Context, metadata: Metadata): Intent {
            return Intent(ctx, javaClass<NewsThreadActivity>())
                    .putExtra(EXTRA_NEWSTHREAD_ID, metadata.id ?: -1)
                    .putExtra(EXTRA_NEWSTHREAD_TITLE, metadata.title)
                    .putExtra(EXTRA_NEWSTHREAD_TEXT, metadata.text)
                    .putExtra(EXTRA_NEWSTHREAD_BY, metadata.by)
                    .putExtra(EXTRA_NEWSTHREAD_TIME, metadata.time)
                    .putExtra(EXTRA_NEWSTHREAD_SCORE, metadata.score)
                    .putExtra(EXTRA_NEWSTHREAD_LINK, metadata.link)

        }
    }

}
