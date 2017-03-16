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
import org.jetbrains.anko.AnkoLogger
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.BindingViewHolder
import se.ntlv.newsbringer.adapter.GenericRecyclerViewAdapter
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.network.RowItem
import se.ntlv.newsbringer.thisShouldNeverHappen


class CommentsAdapterWithHeader(seed: AdapterModelCollection<RowItem>?,
                                private val nestingPaddingIncrement: Int,
                                private val headerClickListener: (View) -> Unit,
                                private val navigator: Navigator) : GenericRecyclerViewAdapter<RowItem, BindingViewHolder<RowItem>>(seed), AnkoLogger {

    override fun getItemViewType(position: Int): Int = when (position) {
        0 -> R.layout.header_item
        else -> R.layout.comment_item
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BindingViewHolder<RowItem> {
        val root = LayoutInflater.from(parent!!.context).inflate(viewType, parent, false)

        return when (viewType) {
            R.layout.comment_item -> RowHolder(root, nestingPaddingIncrement, navigator)
            R.layout.header_item -> HeaderHolder(root, headerClickListener, navigator)
            else -> thisShouldNeverHappen("$viewType cannot be resolved")
        }
    }

    fun findInDataSet(start: Int,
                      predicate: (RowItem) -> Boolean,
                      movementMethod: (Int) -> Int): Int {

        val localDataRef = data ?: return start

        var currentPosition = movementMethod(start)
        val dataRange = 0..localDataRef.size - 1

        while (currentPosition in dataRange) when {
            predicate(localDataRef[currentPosition]) -> return currentPosition
            else -> currentPosition = movementMethod(currentPosition)
        }
        return start
    }
}

fun TextView.setHtml(html: String, navigator: Navigator) {
    val sequence = fromHtml(html)
    val spanBuilder = SpannableStringBuilder(sequence)
    val urls = spanBuilder.getSpans(0, sequence.length, URLSpan::class.java)
    for (span in urls) {
        val start = spanBuilder.getSpanStart(span)
        val end = spanBuilder.getSpanEnd(span)
        val flags = spanBuilder.getSpanFlags(span)
        val url = span.url

        val clickable = object : ClickableSpan() {
            override fun onClick(ignored: View?) {
                navigator.goToLink(url)
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



