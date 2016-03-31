package se.ntlv.newsbringer.comments

import android.content.Context
import android.database.Cursor
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.HeaderCursorRecyclerViewAdapter
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.database.getInt
import se.ntlv.newsbringer.database.getLong
import se.ntlv.newsbringer.database.getString

open class CommentsAdapter(ctx: Context, manager: LinearLayoutManager) :
        HeaderCursorRecyclerViewAdapter<CommentsAdapter.HeaderHolder, CommentsAdapter.RowHolder>(
                HeaderHolder::class.java,
                RowHolder::class.java
        ) {

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
        viewHolder.commentCount.text = mCommentCount
        viewHolder.self.setOnClickListener(headerClickListener)

    }

    override fun onBindRowViewHolder(viewHolder: RowHolder, cursor: Cursor) {
        val ancestorCount = cursor.getInt(CommentsTable.COLUMN_ANCESTOR_COUNT)
        val padding = (pxPadding * ancestorCount).toInt()
        viewHolder.self.setPadding(padding, viewHolder.self.paddingTop, viewHolder.self.paddingRight, viewHolder.self.paddingBottom)

        viewHolder.colorView.setBackgroundResource(color[ancestorCount.mod(color.size)])

        viewHolder.by.text = "${cursor.position} - ${cursor.getString(CommentsTable.COLUMN_BY)}"
        viewHolder.time.text = cursor.getString(CommentsTable.COLUMN_TIME)
        viewHolder.text.htmlText = cursor.getString(CommentsTable.COLUMN_TEXT)
        viewHolder.id = cursor.getLong(CommentsTable.COLUMN_ID)

        val kidsString = cursor.getString(CommentsTable.COLUMN_KIDS)
        val count = if (kidsString.isNotEmpty()) {
            kidsString.split(',').size.toString()
        } else {
            ""
        }
        viewHolder.kids.text = count

        viewHolder.self.setOnLongClickListener { longClickListener?.invoke(viewHolder, viewHolder.id) ?: false }
    }

    private var mTitle: String = ""
    private var mText: String = ""
    private var mBy: String = ""
    private var mTime: String = ""
    private var mScore: String = ""

    private val mManager = manager

    private val HEADER_VIEW = 0
    private val COMMENTS_VIEW = 1

    var mCommentCount: String = ""

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        val countLocal = when {
            count > 0 -> count + 1
            else -> 0
        }
        return countLocal
    }

    override fun getItemViewType(position: Int) = if (position == 0) HEADER_VIEW else COMMENTS_VIEW

    var longClickListener: ((RecyclerView.ViewHolder?, Long?) -> Boolean)? = null

    var headerClickListener: ((View?) -> Unit)? = null

    val pxPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, ctx.resources.displayMetrics).toInt()

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
            return this.text as String
        }
        set(source: String) {
            this.text = Html.fromHtml(source)
        }

    class RowHolder(root: View) : RecyclerView.ViewHolder(root) {
        val self = root
        val text = root.findViewById(R.id.text) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as TextView
        var id: Long? = null
        val kids = root.findViewById(R.id.kids) as TextView
        val colorView = root.findViewById(R.id.color_band)

    }

    class HeaderHolder(root: View) : RecyclerView.ViewHolder(root) {
        val self = root
        val title = root.findViewById(R.id.title) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as DateView
        val score = root.findViewById(R.id.score) as TextView
        val commentCount = root.findViewById(R.id.comment_count) as TextView
        val text = root.findViewById(R.id.text) as TextView

    }

    fun findPreviousTopLevelFrom(): Int {
        val firstVisible = mManager.findFirstVisibleItemPosition()
        return super.findInDataSet(firstVisible, { it.isBeforeFirst }, { it.moveToPrevious() })
    }

    fun findNextTopLevel(): Int {
        val currentPos = mManager.findLastVisibleItemPosition()
        return super.findInDataSet(currentPos, { it.isAfterLast }, { it.moveToNext() })
    }

    fun updateHeader(postTitle: String, text: String, by: String, time: String, score: String) {
        mTitle = postTitle
        mText = text
        mBy = by
        mTime = time
        mScore = score
        notifyItemChanged(0)
    }
}



