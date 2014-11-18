package se.ntlv.newsbringer

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.database.Cursor
import android.content.Loader
import se.ntlv.newsbringer.network.Metadata
import android.widget.TextView
import kotlin.properties.Delegates
import android.widget.ListView
import android.content.CursorLoader
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.network.DataPullPushService
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.view.ViewGroup
import android.support.v4.widget.SwipeRefreshLayout


public class NewsThreadActivity : Activity(), AbstractCursorLoaderCallbacks {

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (args == null || args.getLong(LOADER_ARGS_ID, -1L) == -1L) {
            throw IllegalArgumentException("Cannot instantiate loader will null arguments or missing arguments")
        }
        val projection = array(CommentsTable.COLUMN_BY, CommentsTable.COLUMN_TEXT, CommentsTable.COLUMN_TIME, CommentsTable.COLUMN_ORDINAL, CommentsTable.COLUMN_ID)
        val selection = "${CommentsTable.COLUMN_PARENT}=?"
        val selectionArgs = array(args.getLong(LOADER_ARGS_ID).toString())
        val sorting = CommentsTable.COLUMN_ORDINAL + " ASC"
        return CursorLoader(this, NewsContentProvider.CONTENT_URI_COMMENTS, projection, selection, selectionArgs, sorting)
    }

    override fun getOnLoadFinishedCallback(): ((Cursor?) -> Unit)? = { mSwipeView.setRefreshing(false) }
    override fun getOnLoaderResetCallback(): ((t: Loader<Cursor>?) -> Unit)? = { mSwipeView.setRefreshing(false) }

    override val mAdapter: CommentsListAdapter by Delegates.lazy {
        CommentsListAdapter(this, R.layout.list_item_comment, null, 0)
    }

    private val mSwipeView: SwipeRefreshLayout by Delegates.lazy {
        findViewById(R.id.swipe_view) as SwipeRefreshLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<Activity>.onCreate(savedInstanceState)


        val args = getIntent().getExtras()

        val newsthreadId = args.getLong(EXTRA_NEWSTHREAD_ID)

        if (newsthreadId == 0L) {
            throw IllegalArgumentException("Cannot show newsthread without id")
        }
        setContentView(R.layout.activity_swipe_refresh_list_view_layout)
        val loaderArgs = Bundle()
        loaderArgs.putLong(LOADER_ARGS_ID, newsthreadId)
        getLoaderManager().initLoader(0, loaderArgs, this)
        mSwipeView.setOnRefreshListener { refresh(newsthreadId, true, true) }
        mSwipeView.setColorScheme(android.R.color.holo_blue_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light)

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

        val listView = mSwipeView.findViewById(R.id.list_view) as ListView
        val headerView: LinearLayout = LayoutInflater.from(this).inflate(R.layout.list_header_newsthread, listView, false) as LinearLayout

        array.forEach { findViewAndSetText(headerView, it.first, it.second) }

        listView.addHeaderView(headerView) //important to call before setAdapter if SDK_LEVEL < KITKAT
        listView.setAdapter(mAdapter)

        refresh(newsthreadId)
    }

    fun findViewAndSetText(root: ViewGroup, id: Int, text: String) {
        val view = root.findViewById(id)
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

        val LOADER_ARGS_ID: String = "${TAG}loader_args_id"

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

    fun refresh(id: Long, isCallFromSwipeView: Boolean = false, disallowFetchSkip: Boolean = false) {
        if (!isCallFromSwipeView) {
            mSwipeView.setRefreshing(true)
        }
        DataPullPushService.startActionFetchComments(this, id, disallowFetchSkip)
    }


}
