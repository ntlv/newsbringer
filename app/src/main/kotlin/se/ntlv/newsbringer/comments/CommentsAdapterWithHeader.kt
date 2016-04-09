package se.ntlv.newsbringer.comments

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.find
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.GenericWithHeaderRecyclerViewAdapter
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.network.CommentUiData

class CommentsAdapterWithHeader(val commentNestingPaddingIncrement : Int) : GenericWithHeaderRecyclerViewAdapter<
        CommentUiData,
        CommentsAdapterWithHeader.HeaderHolder,
        CommentsAdapterWithHeader.RowHolder>(HeaderHolder::class.java, RowHolder::class.java) {

    override val deltaUpdatingEnabled = false

    override fun onCreateHeaderViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder =
            HeaderHolder(LayoutInflater.from(parent?.context).inflate(R.layout.list_header_newsthread, parent, false))

    override fun onCreateRowViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder =
            RowHolder(LayoutInflater.from(parent?.context).inflate(R.layout.list_item_comment, parent, false))

    override fun onBindHeaderViewHolder(viewHolder: HeaderHolder) {
        viewHolder.title.text = mTitle
        viewHolder.by.text = mBy
        viewHolder.time.text = mTime
        if (mText.isNullOrEmpty()) {
            viewHolder.text.visibility = View.GONE
        } else {
            viewHolder.text.visibility = View.VISIBLE
            viewHolder.text.htmlText = mText
        }
        viewHolder.score.text = mScore
        viewHolder.commentCount.text = mDescendantsCount
        viewHolder.self.setOnClickListener(headerClickListener)
    }

    override fun onBindRowViewHolder(viewHolder: RowHolder, data: CommentUiData) {

        val ancestorCount = data.ancestorCount
        val padding = commentNestingPaddingIncrement * ancestorCount
        viewHolder.self.setPadding(padding, viewHolder.self.paddingTop, viewHolder.self.paddingRight, viewHolder.self.paddingBottom)

        viewHolder.colorView.setBackgroundResource(color[ancestorCount.mod(color.size)])

        viewHolder.by.text = "${data.position} - ${data.by}"
        viewHolder.time.text = data.time.toString()
        viewHolder.text.htmlText = data.text
        viewHolder.id = data.id

        val kidsString = data.kids
        val count = if (kidsString.isNullOrBlank()) 0 else kidsString.split(',').size
        viewHolder.kids.text = count.toString()
    }

    private var mTitle: String = ""
    private var mText: String = ""
    private var mBy: String = ""
    private var mTime: String = ""
    private var mScore: String = ""
    private var mDescendantsCount = ""

    private val HEADER_VIEW = 0
    private val COMMENTS_VIEW = 1

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        val countLocal = when {
            count > 0 -> count + 1
            mTitle.isNotBlank()
                    || mText.isNotBlank()
                    || mBy.isNotBlank()
                    || mTime.isNotBlank()
                    || mScore.isNotBlank()
                    || mDescendantsCount.isNotBlank() -> 1
            else -> 0
        }
        return countLocal
    }

    override fun getItemViewType(position: Int) = if (position == 0) HEADER_VIEW else COMMENTS_VIEW

    var headerClickListener: ((View?) -> Unit)? = null

    val color = arrayOf(
            R.color.cyan,
            R.color.green,
            R.color.orange,
            R.color.pink,
            R.color.purple,
            R.color.red,
            R.color.teal,
            R.color.yellow
    )

    var TextView.htmlText: String
        get() {
            return text as String
        }
        set(source: String) {
            text = Html.fromHtml(source)
            movementMethod = LinkMovementMethod.getInstance()
        }

    class RowHolder(root: View) : RecyclerView.ViewHolder(root) {
        val self = root
        val text = root.find<TextView>(R.id.text)
        val by = root.find<TextView>(R.id.by)
        val time = root.find<DateView>(R.id.time)
        var id: Long? = null
        val kids = root.find<TextView>(R.id.kids)
        val colorView = root.find<View>(R.id.color_band)

    }

    class HeaderHolder(root: View) : RecyclerView.ViewHolder(root) {
        val self = root
        val title = root.find<TextView>(R.id.title)
        val by = root.find<TextView>(R.id.by)
        val time = root.find<DateView>(R.id.time)
        val score = root.find<TextView>(R.id.score)
        val commentCount = root.find<TextView>(R.id.comment_count)
        val text = root.find<TextView>(R.id.text)

    }

    fun updateHeader(postTitle: String, text: String, by: String, time: String, score: String, descendantsCount: String) {
        mTitle = postTitle
        mText = text
        mBy = by
        mTime = time
        mScore = score
        mDescendantsCount = descendantsCount
        notifyItemChanged(0)
    }
}



