package se.ntlv.newsbringer.comments

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onLongClick
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.newsthreads.NewsThreadsActivity
import kotlin.LazyThreadSafetyMode.NONE


interface CommentsViewBinder {
    fun indicateDataLoading(isLoading: Boolean): Unit

    fun updateHeader(postTitle: String, text: String, by: String, time: String, score: String, link: String)

    fun updateComments(data: Cursor?)

    val context: Context
}

class CommentsActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener, CommentsViewBinder {

    companion object {
        private val TAG: String = CommentsActivity::class.java.simpleName

        val EXTRA_NEWSTHREAD_ID: String = "${TAG}extra_news_thread_id"

        fun getIntent(ctx: Context, id: Long): Intent {
            return Intent(ctx, CommentsActivity::class.java).putExtra(EXTRA_NEWSTHREAD_ID, id)
        }
    }

    private val mNavigateUp = {
        val previous = mAdapter.findPreviousTopLevelFrom()
        mManager.scrollToPositionWithOffset(previous, 0)
    }

    private val mNavigateDown = {
        val next = mAdapter.findNextTopLevel()
        mManager.scrollToPositionWithOffset(next, 0)
    }

    private val mManager: LinearLayoutManager by lazy(NONE) { LinearLayoutManager(this) }

    private val mAdapter: CommentsAdapter by lazy(NONE) { CommentsAdapter(this, mManager) }

    private val mSwipeView: SwipeRefreshLayout by lazy(NONE) { find<SwipeRefreshLayout>(R.id.swipe_view) }

    private val mRecyclerView: RecyclerView by lazy(NONE) { find<RecyclerView>(R.id.recycler_view) }

    private val mAppBar: AppBarLayout by lazy(NONE) { find<AppBarLayout>(R.id.appbar) }

    val mItemId by lazy(NONE) {
        val idFromUri: Long? = intent.data?.getQueryParameter("id")?.toLong()
        val idFromExtras: Long? = intent.extras?.getLong(EXTRA_NEWSTHREAD_ID)
        idFromUri ?: (idFromExtras ?: -1L)
    }

    val presenter: CommentsPresenter by lazy(NONE) {
        val interactor = CommentsInteractor(this, loaderManager, mItemId, Navigator(this))
        CommentsPresenter(this, interactor)
    }

    val fab : FloatingActionButton by lazy(NONE) {
        find<FloatingActionButton>(R.id.fab)
    }

    //VIEW BINDER IMPLEMENTATION
    override fun indicateDataLoading(isLoading: Boolean) {
        mSwipeView.isRefreshing = isLoading
    }

    override fun updateHeader(postTitle: String, text: String, by: String, time: String, score: String, link: String) {
        title = postTitle
        mAdapter.headerClickListener = { presenter.onHeaderClick() }
        mAdapter.updateHeader(postTitle, text, by, time, score)
    }

    override val context: Context
        get() = this

    override fun updateComments(data: Cursor?) {
        mAdapter.swapCursor(data)
        mAdapter.mCommentCount = data?.count?.toString() ?: "0"
    }

    //APP BAR CALLBACKS
    override fun onOffsetChanged(container: AppBarLayout?, verticalOffset: Int) {
        mSwipeView.isEnabled = 0.equals(verticalOffset)
    }

    //ANDROID ACTIVITY CALLBACKS
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if ((-1L).equals(mItemId)) {
            Toast.makeText(this, "Broken link, loading main activity", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, NewsThreadsActivity::class.java)
            startActivity(intent)
            finish()
        }

        setContentView(R.layout.activity_linear_vertical_content)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true);

        mSwipeView.setOnRefreshListener { presenter.refreshData(false, true) }
        mSwipeView.setColorSchemeResources(R.color.primary_color)

        mRecyclerView.layoutManager = mManager
        mRecyclerView.adapter = mAdapter
        mAdapter.longClickListener = { view, id -> presenter.onCommentLongClick(id) }

        presenter.restoreTemporaryState(savedInstanceState)

        fab.onClick { mNavigateDown() }
        fab.onLongClick { mNavigateUp(); true }
    }

    override fun onStart() {
        super.onStart()
        presenter.onViewReady()
    }

    override fun onResume() {
        super.onResume();
        mAppBar.addOnOffsetChangedListener(this);
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        presenter.saveTemporaryState(bundle)
    }

    override fun onPause() {
        super.onPause();
        mAppBar.removeOnOffsetChangedListener(this);
    }

    override fun onStop() {
        super.onStop()
        presenter.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.comments, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.refresh -> {
                presenter.refreshData(true, false); true
            }
            R.id.share_story -> {
                presenter.onShareStoryClicked(); true
            }
            R.id.share_comments -> {
                presenter.onShareCommentsClicked(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}



