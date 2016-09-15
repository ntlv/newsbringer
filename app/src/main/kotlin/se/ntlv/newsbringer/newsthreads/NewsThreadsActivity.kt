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
import se.ntlv.newsbringer.application.YcReaderApplication
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.database.DataFrontPage
import se.ntlv.newsbringer.database.Database
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE


class NewsThreadsActivity : AppCompatActivity(), AnkoLogger, SearchView.OnQueryTextListener {

    @Inject lateinit var database: Database

    private lateinit var mAdapter: NewsThreadAdapter
    private lateinit var mPresenter : NewsThreadsPresenter

    private val mUiBinder: UiBinder by lazy(NONE) {
        UiBinder(this, { mPresenter.refreshData() }, LinearLayoutManager(this), mAdapter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        YcReaderApplication.applicationComponent().inject(this)

        setContentView(R.layout.activity_linear_vertical_content)

        val onClick = { it: NewsThreadAdapter.ViewHolder -> mPresenter.onItemClick(it.id!!) }
        val onLongClick = { it: NewsThreadAdapter.ViewHolder -> mPresenter.onItemLongClick(it.id!!, it.isStarred!!); true }

        val data: DataFrontPage? = savedInstanceState?.getParcelable<DataFrontPage>(dataTag)

        mAdapter = NewsThreadAdapter(R.layout.list_item_news_thread, onClick, onLongClick, data)
        mPresenter = NewsThreadsPresenter(mUiBinder, Navigator(this), NewsThreadsInteractor(this, database, data?.base))
        mAdapter.facilitator = mPresenter

        setSupportActionBar(find<Toolbar>(R.id.toolbar))
        title = getString(R.string.frontpage)
    }

    override fun onStart() {
        super.onStart()
        mUiBinder.start()
    }

    val dataTag = "NewsThreadActivity.data"

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val data = mAdapter.getConcreteData()
        outState.putParcelable(dataTag, data)
    }

    override fun onStop() {
        super.onStop()
        mUiBinder.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val refreshImage = ImageView(this)
        refreshImage.padding = dip(8)
        refreshImage.imageResource = R.drawable.ic_refresh_24dp
        val refreshItem = menu.findItem(R.id.refresh)
        refreshItem.actionView = refreshImage
        refreshItem.actionView.onClick { menu.performIdentifierAction(refreshItem.itemId, 0) }
        mUiBinder.refreshButtonManager = RefreshButtonAnimator(refreshItem, refreshImage)


        val item = menu.findItem(R.id.search)
        val searchView = MenuItemCompat.getActionView(item) as SearchView
        searchView.setOnQueryTextListener(this)
        mPresenter.onViewReady()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.refresh -> mPresenter.refreshData()
            R.id.toggle_show_starred_only -> mPresenter.toggleShowOnlyStarred()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        mPresenter.filter(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String?) = true
}

