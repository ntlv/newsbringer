package se.ntlv.newsbringer.newsthreads

import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import org.jetbrains.anko.*
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.newsthreads.NewsThreadAdapter.ViewHolder
import kotlin.LazyThreadSafetyMode.NONE


class NewsThreadsActivity : AppCompatActivity(), AnkoLogger, SearchView.OnQueryTextListener {

    private val mAdapter: NewsThreadAdapter by lazy(NONE) {
        val onClick = { it: ViewHolder -> mPresenter.onItemClick(it.id!!) }
        val onLongClick = { it: ViewHolder -> mPresenter.onItemLongClick(it.id!!); true }
        NewsThreadAdapter(R.layout.list_item_news_thread, onClick, onLongClick)
    }

    private val mUiBinder: UiBinder by lazy(NONE) {
        UiBinder(this, { mPresenter.refreshData() }, LinearLayoutManager(this), mAdapter)
    }

    private val mPresenter by lazy(NONE) {
        val instance = NewsThreadsPresenter(mUiBinder, Navigator(this), NewsThreadsInteractor(this, loaderManager))
        mAdapter.facilitator = instance
        instance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //the following line looks like bs but see http://stackoverflow.com/a/21328568/1131180 for explanation
        info("Init with  loaderManager@${loaderManager.hashCode()}")

        setContentView(R.layout.activity_linear_vertical_content)

        val toolbar = find<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        title = getString(R.string.frontpage)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val refreshImage = ImageView(this)
        refreshImage.padding = dip(8)
        refreshImage.imageResource = R.drawable.ic_action_refresh
        mUiBinder.refreshButtonManager = RefreshButtonAnimator(menu.findItem(R.id.refresh), refreshImage)
        val item = menu.findItem(R.id.search)
        val searchView = MenuItemCompat.getActionView(item) as SearchView
        searchView.setOnQueryTextListener(this)
        mPresenter.onViewReady()
        return true
    }

    override fun onStart() {
        super.onStart()
        mUiBinder.start()
    }

    override fun onStop() {
        super.onStop()
        mUiBinder.stop()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.refresh -> {
                mPresenter.refreshData()
                true
            }
            R.id.toggle_show_starred_only -> {
                mPresenter.toggleShowOnlyStarred()
                true
            }
            R.id.search -> super.onOptionsItemSelected(item)
            else -> throw IllegalArgumentException()
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        mPresenter.filter(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String?) = true
}

