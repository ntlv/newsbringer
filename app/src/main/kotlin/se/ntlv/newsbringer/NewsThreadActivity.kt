package se.ntlv.newsbringer

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.database.Cursor
import android.content.Loader
import se.ntlv.newsbringer.network.NewsThread.Metadata
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
import android.text.Html
import android.net.Uri
import android.view.View
import java.util.HashSet
import android.util.Log
import android.widget.Toast


public class NewsThreadActivity : Activity(), AbstractCursorLoaderCallbacks {

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (args == null || args.getLong(LOADER_ARGS_ID, -1L) == -1L) {
            throw IllegalArgumentException("Cannot instantiate loader will null arguments or missing arguments")
        }
        val projection = array(
                CommentsTable.COLUMN_BY,
                CommentsTable.COLUMN_TEXT,
                CommentsTable.COLUMN_TIME,
                CommentsTable.COLUMN_ORDINAL,
                CommentsTable.COLUMN_ID,
                CommentsTable.COLUMN_ANCESTOR_COUNT,
                CommentsTable.COLUMN_KIDS

        )
        val selection = "${CommentsTable.COLUMN_PARENT}=?"
        val selectionArgs = array(args.getLong(LOADER_ARGS_ID).toString())
        val sorting = CommentsTable.COLUMN_ORDINAL + " ASC"
        return CursorLoader(this, NewsContentProvider.CONTENT_URI_COMMENTS, projection, selection, selectionArgs, sorting)
    }

    override fun getOnLoadFinishedCallback(): ((Cursor?) -> Unit)? = {
        if(it != null && it.getCount() > 0) {
            mSwipeView.setRefreshing(false)
            (findViewById(R.id.comment_count) as? TextView)?.setText(it.getCount().toString())
        }

    }
    override fun getOnLoaderResetCallback(): ((t: Loader<Cursor>?) -> Unit)? = { mSwipeView.setRefreshing(false) }

    override val mAdapter: CommentsListAdapter by Delegates.lazy {
        CommentsListAdapter(this, R.layout.list_item_comment, null, 0)
    }

    private val mSwipeView: SwipeRefreshLayout by Delegates.lazy {
        findViewById(R.id.swipe_view) as SwipeRefreshLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<Activity>.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            val positions = savedInstanceState.getLongArray(STATE_HANDLED_POSITIONS)
            positions.toCollection(handledPositions)
        }

        val args = getIntent().getExtras()

        val newsthreadId = args.getLong(EXTRA_NEWSTHREAD_ID)

        if (newsthreadId == 0L) {
            throw IllegalArgumentException("Cannot show newsthread without id")
        }

        setContentView(R.layout.activity_swipe_refresh_list_view_layout)
        setTitle(args.getString(EXTRA_NEWSTHREAD_TITLE))

        val loaderArgs = Bundle()
        loaderArgs.putLong(LOADER_ARGS_ID, newsthreadId)
        getLoaderManager().initLoader(0, loaderArgs, this)
        mSwipeView.setOnRefreshListener { refresh(newsthreadId, true, true) }
        mSwipeView.setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        )

        val title = args.getString(EXTRA_NEWSTHREAD_TITLE)
        val text = args.getString(EXTRA_NEWSTHREAD_TEXT)
        val by = args.getString(EXTRA_NEWSTHREAD_BY)
        val time = args.getString(EXTRA_NEWSTHREAD_TIME)
        val score = args.getString(EXTRA_NEWSTHREAD_SCORE)
        val link = args.getString(EXTRA_NEWSTHREAD_LINK)
        val commentCount = args.getLong(EXTRA_NEWSTHREAD_KID_COUNT)

        val array = array(
                Triple(R.id.title, title, false),
                Triple(R.id.text, text, true),
                Triple(R.id.by, by, false),
                Triple(R.id.time, time, false),
                Triple(R.id.score, score, false),
                Triple(R.id.comment_count, commentCount.toString(), false)
        )

        val listView = mSwipeView.findViewById(R.id.list_view) as ListView
        listView.setDivider(null) //ugly, but disables dividers
        val headerView = LayoutInflater.from(this).inflate(R.layout.list_header_newsthread, listView, false) as LinearLayout
        headerView.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }

        array.forEach { findViewAndSetText(headerView, it.first, it.second, it.third) }

        listView.addHeaderView(headerView) //important to call before setAdapter if SDK_LEVEL < KITKAT
        listView.setAdapter(mAdapter)
        listView.setOnItemClickListener {(adapterView, view, i, l) -> fetchChildComments(l, view, newsthreadId) }

        refresh(newsthreadId)
    }

    fun findViewAndSetText(root: ViewGroup, id: Int, text: String, isHtml: Boolean) {
        val view = root.findViewById(id)
        if (view is TextView) {
            if (isHtml) {
                view.setText(Html.fromHtml(text))
            } else {
                view.setText(text)
            }
        }
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
        val EXTRA_NEWSTHREAD_KID_COUNT = "${TAG}extra_news_thread_kids"

        val STATE_HANDLED_POSITIONS: String = "${TAG}_handled_positions"

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
                    .putExtra(EXTRA_NEWSTHREAD_KID_COUNT, metadata.kidCount)

        }
    }

    fun refresh(id: Long, isCallFromSwipeView: Boolean = false, disallowFetchSkip: Boolean = false) {
        if (!isCallFromSwipeView) {
            mSwipeView.setRefreshing(true)
        }
        DataPullPushService.startActionFetchComments(this, id, disallowFetchSkip)
    }

    val handledPositions by Delegates.lazy {
        HashSet<Long>()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super<Activity>.onSaveInstanceState(bundle)

        val out = LongArray(handledPositions.size())
        handledPositions.withIndex().forEach { out.set(it.index, it.value) }

        bundle.putLongArray(STATE_HANDLED_POSITIONS, out)
    }

    fun fetchChildComments(commentId: Long, view: View, threadId: Long) {
        if (commentId !in handledPositions) {
            handledPositions.add(commentId)
            val tag = view.getTag()
            if (tag is CommentsListAdapter.ViewHolder && tag.id != null) {
                DataPullPushService.startActionFetchChildComments(this, tag.id ?: -1L, threadId)
            }
        } else {
            Log.d(TAG, "Ignoring click event on view $commentId")
        }
    }
}
