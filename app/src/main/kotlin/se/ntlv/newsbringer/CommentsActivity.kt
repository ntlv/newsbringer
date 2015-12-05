package se.ntlv.newsbringer

import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.network.DataPullPushService
import java.util.*


public class CommentsActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {
    override fun onOffsetChanged(p0: AppBarLayout?, p1: Int) {
        mSwipeView.isEnabled = p1 == 0
    }

    override fun onResume() {
        super.onResume();
        mAppBar.addOnOffsetChangedListener(this);
    }

    override fun onPause() {
        super.onPause();
        mAppBar.removeOnOffsetChangedListener(this);
    }

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
        mRecyclerView.visibility = View.VISIBLE
        mProgressView.visibility = View.INVISIBLE
        mAdapter.mCommentCount = data.count.toString()
    }

    private var mShareStory = { toast() }
    private var mShareComments = { toast() }
    private var mNavigateUp = { toast() }
    private var mNavigateDown = { toast() }

    private fun toast() = Toast.makeText(this, "Data not yet loaded", Toast.LENGTH_SHORT).show()

    private fun updateHeader(data: Cursor) {
        val postTitle = data.getString(PostTable.COLUMN_TITLE)
        title = postTitle

        mRecyclerView.visibility = View.VISIBLE
        mProgressView.visibility = View.INVISIBLE
        val link = data.getString(PostTable.COLUMN_URL)
        mShareStory = { shareLink(postTitle, link) }
        mShareComments = { shareLink(postTitle, "https://news.ycombinator.com/item?id=$mItemId") }
        mAdapter.headerClickListener = {
            if (link.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }
        val by = data.getString(PostTable.COLUMN_BY)
        val text = data.getString(PostTable.COLUMN_TEXT)
        val time = data.getString(PostTable.COLUMN_TIMESTAMP)
        val score = data.getString(PostTable.COLUMN_SCORE)
        mAdapter.updateHeader(postTitle, text, by, time, score)

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
            throw IllegalArgumentException("Passed null loader in reset")
        }
        when (loader.id) {
            R.id.loader_comments_header -> {
                mAdapter.headerClickListener = {}
                mAdapter.updateHeader("", "", "", "", "")
            }
            R.id.loader_comments_comments -> {
                mSwipeView.isRefreshing = false
                mAdapter.swapCursor(null)
            }
        }
    }

    private val mManager: LinearLayoutManager by lazy(LazyThreadSafetyMode.NONE) {
        LinearLayoutManager(this)
    }

    private val mAdapter: CommentsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        CommentsAdapter(this, mManager)
    }

    private val mSwipeView: SwipeRefreshLayout by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.swipe_view) as SwipeRefreshLayout
    }

    private val mProgressView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.progress_view)
    }

    private val mRecyclerView: RecyclerView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.recycler_view) as RecyclerView
    }

    val mItemId by lazy(LazyThreadSafetyMode.NONE) {
        val idFromUri: Long? = intent.data?.getQueryParameter("id")?.toLong()
        val idFromExtras: Long? = intent.extras?.getLong(EXTRA_NEWSTHREAD_ID)
        idFromUri ?: (idFromExtras ?: -1L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if ((-1L).equals(mItemId)) {
            Toast.makeText(this, "Broken link, loading main activity", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        if (savedInstanceState != null) {
            val positions = savedInstanceState.getLongArray(STATE_HANDLED_POSITIONS)
            positions.toCollection(handledPositions)
        }

        setContentView(R.layout.activity_linear_vertical_content)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        mRecyclerView.visibility = View.INVISIBLE
        mProgressView.visibility = View.VISIBLE

        val loaderArgs = Bundle()
        loaderArgs.putLong(LOADER_ARGS_ID, mItemId)
        loaderManager.initLoader(R.id.loader_comments_comments, loaderArgs, this)
        loaderManager.initLoader(R.id.loader_comments_header, loaderArgs, this)

        mSwipeView.setOnRefreshListener { refreshComments(false, true) }
        mSwipeView.setColorSchemeResources(R.color.primary_color)

        mRecyclerView.layoutManager = mManager
        mRecyclerView.adapter = mAdapter
        mAdapter.longClickListener = { view, id -> fetchChildComments(id, view as? CommentsAdapter.RowHolder?, mItemId); true }

        mNavigateUp = {
            val previous = mAdapter.findPreviousTopLevelFrom()
            mRecyclerView.smoothScrollToPosition(previous)
        }
        mNavigateDown = {
            val next = mAdapter.findNextTopLevel()
            mRecyclerView.smoothScrollToPosition(next)
        }
    }

    private val mAppBar: AppBarLayout by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.appbar) as AppBarLayout
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
            R.id.navigate_previous_top_comment -> {
                mNavigateUp(); true
            }
            R.id.navigate_next_top_comment -> {
                mNavigateDown(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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

        val out = LongArray(handledPositions.size)
        handledPositions.withIndex().forEach { out[it.index] = it.value }

        bundle.putLongArray(STATE_HANDLED_POSITIONS, out)
    }

    fun fetchChildComments(commentId: Long?, view: CommentsAdapter.RowHolder?, threadId: Long) {
        if (commentId == null || view == null) {
            return
        }
        if (commentId !in handledPositions) {
            handledPositions.add(commentId)
            if ( view.id != null) {
                DataPullPushService.startActionFetchChildComments(this, view.id ?: -1L, threadId)
            }
        } else {
            Log.d(TAG, "Ignoring click event on view $commentId")
        }
    }
}
