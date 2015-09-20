package se.ntlv.newsbringer

import android.app.Activity
import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.network.DataPullPushService
import java.util.*


public class CommentsActivity : Activity(), LoaderManager.LoaderCallbacks<Cursor> {

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
                return CursorLoader(this, NewsContentProvider.CONTENT_URI_COMMENTS,
                        projection, selection, selectionArgs, sorting)
            }
            R.id.loader_comments_header -> {
                val selection = "${PostTable.COLUMN_ID}=?"
                val selectionArgs = arrayOf(args.getLong(LOADER_ARGS_ID).toString())
                return CursorLoader(this, NewsContentProvider.CONTENT_URI_POSTS,
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
                    refreshHeader()
                } else {
                    updateHeader(data)
                }

            }
            R.id.loader_comments_comments -> {
                if (!data.moveToFirst()) {
                    refreshComments()
                } else {
                    updateComments(data)
                }
            }
        }
    }

    private fun updateComments(data: Cursor) {
        mSwipeView.isRefreshing = false
        mAdapter.swapCursor(data)
        mListView.visibility = View.VISIBLE
        mProgressView.visibility = View.INVISIBLE
        (findViewById(R.id.comment_count) as? TextView)?.text = data.count.toString()
    }

    private var mShareStory: (() ->Unit) = { toast() }
    private var mShareComments: (() -> Unit) = { toast() }

    private fun toast() = Toast.makeText(this, "Data not yet loaded", Toast.LENGTH_SHORT).show()

    private fun updateHeader(data: Cursor) {
        val title = data.getString(PostTable.COLUMN_TITLE)
        setTitle(title)

        mListView.visibility = View.VISIBLE
        mProgressView.visibility = View.INVISIBLE
        val link = data.getString(PostTable.COLUMN_URL)
        mShareStory = { shareLink(title, link)}
        mShareComments = { shareLink(title, "https://news.ycombinator.com/item?id=$mItemId")}
        mHeaderView.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }

        arrayOf(Triple(R.id.title, title, false),
                Triple(R.id.text, data.getString(PostTable.COLUMN_TEXT), true),
                Triple(R.id.by, data.getString(PostTable.COLUMN_BY), false),
                Triple(R.id.time, data.getString(PostTable.COLUMN_TIMESTAMP), false),
                Triple(R.id.score, data.getString(PostTable.COLUMN_SCORE), false)
        ).forEach { findViewAndSetText(mHeaderView, it.first, it.second, it.third) }
    }

    private fun shareLink(title: String, link: String): Unit {
        val i = Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, title);
        i.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(i, "Share URL"))
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        if (loader == null) {
            return
        }
        val loaderId = loader.id

        if (loaderId == R.id.loader_comments_header) {
            arrayOf(Triple(R.id.title, "Loading reset.", false),
                    Triple(R.id.text, "", true),
                    Triple(R.id.by, "", false),
                    Triple(R.id.time, "", false),
                    Triple(R.id.score, "", false),
                    Triple(R.id.comment_count, "0", false)
            ).forEach { findViewAndSetText(mHeaderView, it.first, it.second, it.third) }

        } else if (loaderId == R.id.loader_comments_comments) {
            mSwipeView.isRefreshing = false
            mAdapter.swapCursor(null)
        }
    }

    fun findViewAndSetText(root: ViewGroup?, id: Int, text: String, isHtml: Boolean) {
        val view = root?.findViewById(id)
        if (view is TextView) {
            if (isHtml) {
                view.text = Html.fromHtml(text)
            } else {
                view.text = text
            }
        }
    }

    private val mAdapter: CommentsListAdapter by lazy(LazyThreadSafetyMode.NONE) {
        CommentsListAdapter(this, R.layout.list_item_comment, null, 0)
    }

    private val mSwipeView: SwipeRefreshLayout by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.swipe_view) as SwipeRefreshLayout
    }

    private val mProgressView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.progress_view)
    }

    private val mListView: ListView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.list_view) as ListView
    }

    private val mHeaderView: LinearLayout by lazy(LazyThreadSafetyMode.NONE) {
        layoutInflater.inflate(R.layout.list_header_newsthread, mListView, false) as LinearLayout
    }

//    private val mSwipeNavView: View by lazy(LazyThreadSafetyMode.NONE) {
//        findViewById(R.id.swipe_navigation_detector)
//    }



    val mItemId by lazy(LazyThreadSafetyMode.NONE) {
        val idFromUri: Long? = intent.data?.getQueryParameter("id")?.toLong()
        val idFromExtras: Long? = intent.extras?.getLong(EXTRA_NEWSTHREAD_ID)
        idFromUri ?: (idFromExtras ?: -1L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if((-1L).equals(mItemId)) {
            Toast.makeText(this, "Broken link, loading main activity", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        if (savedInstanceState != null) {
            val positions = savedInstanceState.getLongArray(STATE_HANDLED_POSITIONS)
            positions.toCollection(handledPositions)
        }

        setContentView(R.layout.activity_swipe_refresh_list_view_layout)

        mListView.visibility = View.INVISIBLE
        mProgressView.visibility = View.VISIBLE

        val loaderArgs = Bundle()
        loaderArgs.putLong(LOADER_ARGS_ID, mItemId)
        loaderManager.initLoader(R.id.loader_comments_comments, loaderArgs, this)
        loaderManager.initLoader(R.id.loader_comments_header, loaderArgs, this)

        mSwipeView.setOnRefreshListener { refreshComments(false, true) }
        mSwipeView.setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        )


        mListView.divider = null //ugly, but disables dividers

        mListView.addHeaderView(mHeaderView) //important to call before setAdapter if SDK_LEVEL < KITKAT
        mListView.adapter = mAdapter
        mListView.setOnItemClickListener { adapterView, view, i, l -> fetchChildComments(l, view, mItemId) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.comments, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.refresh -> {
                refreshComments(); true
            }
            R.id.share_story -> {
                mShareStory(); true
            }
            R.id.share_comments -> {
                mShareComments(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

//    class Swiper(ctx: Context) : OnSwipeTouchDetector() {
//        private val mCtx: Context = ctx
//
//        override fun onSwipeRight(): Boolean {
//            Toast.makeText(mCtx, "Swipe right", Toast.LENGTH_SHORT).show()
//            return true
//        }
//
//        override fun onSwipeLeft(): Boolean {
//            Toast.makeText(mCtx, "Swipe left", Toast.LENGTH_SHORT).show()
//            return true
//        }
//
//        override fun onSwipeTop(): Boolean {
//            Toast.makeText(mCtx, "Swipe top", Toast.LENGTH_SHORT).show()
//            return true
//        }
//
//        override fun onSwipeBottom(): Boolean {
//            Toast.makeText(mCtx, "Swipe bottom", Toast.LENGTH_SHORT).show()
//            return true
//        }
//
//    }

    companion object {
        val TAG: String = CommentsActivity::class.java.simpleName

        val EXTRA_NEWSTHREAD_ID: String = "${TAG}extra_news_thread_id"

        val STATE_HANDLED_POSITIONS: String = "${TAG}_handled_positions"

        val LOADER_ARGS_ID: String = "${TAG}loader_args_id"

        public fun getIntent(ctx: Context, id: Long): Intent {
            return Intent(ctx, CommentsActivity::class.java)
                    .putExtra(EXTRA_NEWSTHREAD_ID, id)
        }
    }

    fun refreshHeader(shouldShowRefreshSpinner: Boolean = true) {
        if (shouldShowRefreshSpinner) {
            mSwipeView.isRefreshing = true
        }
        DataPullPushService.startActionFetchThread(this, mItemId)
    }

    fun refreshComments(shouldShowRefreshSpinner: Boolean = true, disallowFetchSkip: Boolean = false) {
        if (shouldShowRefreshSpinner) {
            mSwipeView.isRefreshing = true
        }
        DataPullPushService.startActionFetchComments(this, mItemId, disallowFetchSkip)
    }

    val handledPositions by lazy(LazyThreadSafetyMode.NONE) {
        HashSet<Long>()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)

        val out = LongArray(handledPositions.size())
        handledPositions.withIndex().forEach { out.set(it.index, it.value) }

        bundle.putLongArray(STATE_HANDLED_POSITIONS, out)
    }

    fun fetchChildComments(commentId: Long, view: View, threadId: Long) {
        if (commentId !in handledPositions) {
            handledPositions.add(commentId)
            val tag = view.tag
            if (tag is CommentsListAdapter.ViewHolder && tag.id != null) {
                DataPullPushService.startActionFetchChildComments(this, tag.id ?: -1L, threadId)
            }
        } else {
            Log.d(TAG, "Ignoring click event on view $commentId")
        }
    }
}
