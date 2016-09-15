package se.ntlv.newsbringer.comments

import android.os.Build
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.browse
import org.jetbrains.anko.find
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.GenericWithHeaderRecyclerViewAdapter
import se.ntlv.newsbringer.adapter.starify
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.database.DataCommentsThread
import se.ntlv.newsbringer.network.RowItem

class CommentsAdapterWithHeader(seed: Data<RowItem>?, val commentNestingPaddingIncrement: Int, val headerClickListener: ((View?) -> Unit)?) :
        GenericWithHeaderRecyclerViewAdapter<
                RowItem,
                RowItem.NewsThreadUiData,
                RowItem.CommentUiData,
                CommentsAdapterWithHeader.HeaderHolder,
                CommentsAdapterWithHeader.RowHolder>(
                seed,
                RowItem.NewsThreadUiData::class.java,
                RowItem.CommentUiData::class.java,
                HeaderHolder::class.java,
                RowHolder::class.java
        ),
        AnkoLogger {

    override fun onCreateHeaderViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder =
            HeaderHolder(LayoutInflater.from(parent?.context).inflate(R.layout.list_header_newsthread, parent, false))

    override fun onCreateRowViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder =
            RowHolder(LayoutInflater.from(parent?.context).inflate(R.layout.list_item_comment, parent, false))

    override fun onBindHeaderViewHolder(viewHolder: HeaderHolder, data: RowItem.NewsThreadUiData) {
        viewHolder.title.text = data.title.starify(data.isStarred)
        viewHolder.by.text = data.by
        viewHolder.time.text = data.time.toString()
        if (data.text.isNullOrEmpty()) {
            viewHolder.text.visibility = View.GONE
        } else {
            viewHolder.text.visibility = View.VISIBLE
            viewHolder.text.html = data.text
        }
        viewHolder.score.text = data.score.toString()
        viewHolder.commentCount.text = data.descendants.toString()
        viewHolder.self.setOnClickListener(headerClickListener)
    }

    override fun onBindRowViewHolder(viewHolder: RowHolder, data: RowItem.CommentUiData) {
        val ancestorCount = data.ancestorCount
        val padding = commentNestingPaddingIncrement * ancestorCount
        viewHolder.self.setPadding(padding, viewHolder.self.paddingTop, viewHolder.self.paddingRight, viewHolder.self.paddingBottom)

        viewHolder.colorView.setBackgroundResource(color[ancestorCount.mod(color.size)])

        viewHolder.by.text = "${data.position} - ${data.by}"
        viewHolder.time.text = data.time.toString()
        viewHolder.text.html = data.text
        viewHolder.id = data.id

        val kidsString = data.kids
        val count = if (kidsString.isNullOrBlank()) 0 else kidsString.split(',').size
        viewHolder.kids.text = count.toString()
    }

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

    var TextView.html: String
        get() = throw IllegalAccessError("Getter not implemented")
        set(html: String) {
            val sequence = fromHtml(html)
            val spanBuilder = SpannableStringBuilder(sequence)
            val urls = spanBuilder.getSpans(0, sequence.length, URLSpan::class.java)
            for (span in urls) {
                val start = spanBuilder.getSpanStart(span)
                val end = spanBuilder.getSpanEnd(span)
                val flags = spanBuilder.getSpanFlags(span)
                val clickable = object : ClickableSpan() {
                    override fun onClick(p0: View?) {
                        context.browse(span.url)
                    }

                }
                spanBuilder.setSpan(clickable, start, end, flags)
                spanBuilder.removeSpan(span)
            }
            text = spanBuilder
            movementMethod = LinkMovementMethod.getInstance()
        }


    @Suppress("DEPRECATION")
    fun fromHtml(html: String): Spanned =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Html.fromHtml(html)
            } else {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
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

    fun getConcreteData(): DataCommentsThread? {
        return data as? DataCommentsThread
    }
}



