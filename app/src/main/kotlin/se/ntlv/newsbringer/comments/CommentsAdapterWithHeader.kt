package se.ntlv.newsbringer.comments

import android.os.Build
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
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.GenericRecyclerViewAdapter
import se.ntlv.newsbringer.adapter.starify
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.database.DataCommentsThread
import se.ntlv.newsbringer.network.RowItem
import se.ntlv.newsbringer.network.RowItem.CommentUiData
import se.ntlv.newsbringer.network.RowItem.NewsThreadUiData
import se.ntlv.newsbringer.thisShouldNeverHappen

class CommentsAdapterWithHeader(seed: Data<RowItem>?,
                                val commentNestingPaddingIncrement: Int,
                                val headerClickListener: ((View) -> Unit)) : GenericRecyclerViewAdapter<RowItem, ViewHolder>(seed), AnkoLogger {

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

    override fun getItemViewType(position: Int): Int = if (position == 0) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent?.context)
        return if (viewType == 0) {
            HeaderHolder(inflater.inflate(R.layout.list_header_newsthread, parent, false))
        } else {
            RowHolder(inflater.inflate(R.layout.list_item_comment, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val item = data!![position]
        when {
            holder is HeaderHolder && item is NewsThreadUiData -> holder.bind(item, headerClickListener)
            holder is RowHolder && item is CommentUiData -> holder.bind(item, commentNestingPaddingIncrement, color)
            else -> thisShouldNeverHappen()
        }
    }

    fun findInDataSet(start: Int,
                      predicate: (RowItem) -> Boolean,
                      movementMethod: (Int) -> Int): Int {

        val localDataRef = data ?: return start

        var currentPosition = movementMethod(start)
        val dataRange = 0..localDataRef.count - 1

        while (currentPosition in dataRange) {

            if (predicate(localDataRef[currentPosition])) {
                return currentPosition
            } else {
                currentPosition = movementMethod(currentPosition)
            }
        }
        return start
    }

    fun getConcreteData(): DataCommentsThread? {
        return data as? DataCommentsThread
    }


    class RowHolder(root: View) : ViewHolder(root) {
        val self = root
        val text = root.find<TextView>(R.id.text)
        val by = root.find<TextView>(R.id.by)
        val time = root.find<DateView>(R.id.time)
        var id: Long? = null
        val kids = root.find<TextView>(R.id.kids)
        val colorView = root.find<View>(R.id.color_band)

        fun bind(data: CommentUiData, commentNestingPaddingIncrement: Int, color: Array<Int>) {
            val ancestorCount = data.ancestorCount
            val padding = commentNestingPaddingIncrement * ancestorCount
            self.setPadding(padding, self.paddingTop, self.paddingRight, self.paddingBottom)

            colorView.setBackgroundResource(color[ancestorCount.mod(color.size)])

            by.text = "${data.position} - ${data.by}"
            time.text = data.time.toString()
            text.html = if (!data.text.isNullOrEmpty()) data.text else "[Removed]"
            id = data.id

            val kidsString = data.kids
            val count = if (kidsString.isNullOrBlank()) 0 else kidsString.split(',').size
            kids.text = count.toString()
        }
    }

    class HeaderHolder(root: View) : ViewHolder(root) {
        val self = root
        val title = root.find<TextView>(R.id.title)
        val by = root.find<TextView>(R.id.by)
        val time = root.find<DateView>(R.id.time)
        val score = root.find<TextView>(R.id.score)
        val commentCount = root.find<TextView>(R.id.comment_count)
        val text = root.find<TextView>(R.id.text)

        fun bind(item: NewsThreadUiData, headerClickListener: ((View) -> Unit)) {
            title.text = item.title.starify(item.isStarred)
            by.text = item.by
            time.text = item.time.toString()
            if (item.text.isNullOrEmpty()) {
                text.visibility = View.GONE
            } else {
                text.visibility = View.VISIBLE
                text.html = item.text
            }
            score.text = item.score.toString()
            commentCount.text = item.descendants.toString()
            self.setOnClickListener(headerClickListener)
        }
    }
}

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



