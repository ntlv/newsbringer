package se.ntlv.newsbringer.comments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import org.jetbrains.anko.*
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.applyAppBarLayoutDependency
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.database.TypedCursor
import se.ntlv.newsbringer.network.RowItem
import se.ntlv.newsbringer.newsthreads.NewsThreadsActivity
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KFunction1


interface CommentsViewBinder {
    fun indicateDataLoading(isLoading: Boolean): Unit

    fun updateContent(data: TypedCursor<RowItem>?)

    val context: Context
}

fun predicate(item: RowItem) = item is RowItem.CommentUiData && item.ancestorCount == 0

class CommentsActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener, CommentsViewBinder {

    private fun generalNav(starter: KFunction1<LinearLayoutManager, Int>, mover: KFunction1<Int, Int>) {
        val start = starter(mManager)
        val predicate: (RowItem) -> Boolean = ::predicate
        val target = mAdapter.findInDataSet(start, predicate, mover)
        if (target != null) {
            mManager.scrollToPositionWithOffset(target, 0)
        }
    }

    private val mNavigateUp = { generalNav(LinearLayoutManager::findFirstVisibleItemPosition, Int::dec) }

    private val mNavigateDown = { generalNav(LinearLayoutManager::findLastVisibleItemPosition, Int::inc) }

    private val mManager: LinearLayoutManager by lazy(NONE) { LinearLayoutManager(this) }

    private val mAdapter: CommentsAdapterWithHeader by lazy(NONE) {
        val padding = applyDimension(COMPLEX_UNIT_DIP, 4f, displayMetrics).toInt()
        CommentsAdapterWithHeader(padding, { presenter.onHeaderClick() })
    }

    private val mSwipeView: SwipeRefreshLayout by lazy(NONE) { find<SwipeRefreshLayout>(R.id.swipe_view) }

    private val mRecyclerView: RecyclerView by lazy(NONE) { find<RecyclerView>(R.id.recycler_view) }

    private val mAppBar: AppBarLayout by lazy(NONE) { find<AppBarLayout>(R.id.appbar) }

    private val mItemId by lazy(NONE) { intent.data?.getQueryParameter("id")!!.toLong() }

    val presenter: CommentsPresenter by lazy(NONE) {
        val interactor = CommentsInteractor(this, loaderManager, mItemId, Navigator(this))
        CommentsPresenter(this, interactor)
    }

    val fab: FloatingActionButton by lazy(NONE) {
        findViewById(R.id.fab) as FloatingActionButton
    }

    //VIEW BINDER IMPLEMENTATION
    override val context: Context = this

    override fun indicateDataLoading(isLoading: Boolean) {
        if (mSwipeView.isRefreshing != isLoading) {
            mSwipeView.isRefreshing = isLoading
        }
        refreshButtonManager.indicateLoading(isLoading)
    }

    override fun updateContent(data: TypedCursor<RowItem>?) {
        val maybeHeader = data?.get(0)
        if (maybeHeader is RowItem.NewsThreadUiData) {
            title = maybeHeader.title
        }
        mAdapter.updateContent(data)
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
            return
        }

        setContentView(R.layout.activity_linear_vertical_content)
        val toolbar = find<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mSwipeView.setOnRefreshListener { presenter.refreshData() }
        mSwipeView.setColorSchemeResources(R.color.accent_color)

        mRecyclerView.layoutManager = mManager
        mRecyclerView.adapter = mAdapter

        fab.applyAppBarLayoutDependency()
        fab.setOnClickListener { mNavigateDown() }
        fab.setOnLongClickListener { mNavigateUp(); true }
    }

    private lateinit var refreshButtonManager: RefreshButtonAnimator

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.comments, menu)
        val refreshImage = ImageView(this)
        refreshImage.padding = dip(8)
        refreshImage.imageResource = R.drawable.ic_action_refresh
        refreshButtonManager = RefreshButtonAnimator(menu.findItem(R.id.refresh), refreshImage)
        presenter.onViewReady()
        return true
    }

    override fun onStart() {
        super.onStart()
        mAppBar.addOnOffsetChangedListener(this)
    }

    override fun onStop() {
        super.onStop()
        mAppBar.removeOnOffsetChangedListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.refresh -> {
                presenter.refreshData(); true
            }
            R.id.share_story -> {
                presenter.onShareStoryClicked(); true
            }
            R.id.share_comments -> {
                presenter.onShareCommentsClicked(); true
            }
            R.id.open_link -> {
                presenter.onHeaderClick(); true
            }
            R.id.add_to_starred -> {
                presenter.addToStarred(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}



