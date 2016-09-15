package se.ntlv.newsbringer.comments

import android.os.Bundle
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.application.YcReaderApplication
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.database.DataCommentsThread
import se.ntlv.newsbringer.database.Database

class CommentsActivity : AppCompatActivity() {

    @Inject lateinit var database: Database

    private lateinit var mAdapter: CommentsAdapterWithHeader
    private lateinit var mPresenter: CommentsPresenter
    private lateinit var mUiBinder : UiBinder

    val dataTag = "CommentsActivity.data"


    private val mItemId by lazy(NONE) { intent.data.getQueryParameter("id")!!.toLong() }

    //ANDROID ACTIVITY CALLBACKS
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if ((-1L).equals(mItemId)) {
            throw IllegalArgumentException()
        }
        YcReaderApplication.applicationComponent().inject(this)

        setContentView(R.layout.activity_linear_vertical_content)

        setSupportActionBar(find<Toolbar>(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val data = savedInstanceState?.getParcelable<DataCommentsThread>(dataTag)

        val padding = applyDimension(COMPLEX_UNIT_DIP, 4f, displayMetrics).toInt()
        mAdapter = CommentsAdapterWithHeader(data, padding, { mPresenter.onHeaderClick() })

        mUiBinder = UiBinder(this, LinearLayoutManager(this), mAdapter)
        val interactor = CommentsInteractor(this, database, mItemId, Navigator(this), data?.base)
        mPresenter = CommentsPresenter(mUiBinder, interactor)

    }

    override fun onStart() {
        super.onStart()
        mUiBinder.start()
    }

    override fun onStop() {
        super.onStop()
        mUiBinder.stop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val data = mAdapter.getConcreteData()
        outState.putParcelable(dataTag, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.destroy()
        mUiBinder.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.comments, menu)
        val refreshImage = ImageView(this)
        refreshImage.padding = dip(8)
        refreshImage.imageResource = R.drawable.ic_refresh_24dp
        val refreshItem = menu.findItem(R.id.refresh)
        refreshItem.actionView = refreshImage
        refreshItem.actionView.onClick { menu.performIdentifierAction(refreshItem.itemId, 0) }
        mUiBinder.refreshButtonManager = RefreshButtonAnimator(refreshItem, refreshImage)
        mUiBinder.refreshButtonManager = RefreshButtonAnimator(menu.findItem(R.id.refresh), refreshImage)
        mPresenter.onViewReady()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.refresh -> mPresenter.refreshData()
            R.id.share_story -> mPresenter.onShareStoryClicked()
            R.id.share_comments -> mPresenter.onShareCommentsClicked()
            R.id.open_link -> mPresenter.onHeaderClick()
            R.id.add_to_starred -> mPresenter.addToStarred()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}



