package se.ntlv.newsbringer

import android.database.Cursor
import android.content.Context
import android.widget.ResourceCursorAdapter
import android.view.View
import android.widget.TextView
import se.ntlv.newsbringer.database.CommentsTable
import android.text.Html
import android.util.TypedValue
import android.widget.LinearLayout
import android.view.ViewGroup

open class CommentsListAdapter(ctx: Context, layout: Int, cursor: Cursor?, flags: Int) :
        ResourceCursorAdapter(ctx, layout, cursor, flags) {

    val pxPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, ctx.getResources().getDisplayMetrics())

    fun <T> Array<T>.getWithLoopingIndex(i: Int): T = this.get(i.mod(this.size()))

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val tag = view.tag
        tag.by.setText(cursor.getString(CommentsTable.COLUMN_BY))
        tag.time.setText(cursor.getString(CommentsTable.COLUMN_TIME))
        tag.text.setHtmlText(cursor.getString(CommentsTable.COLUMN_TEXT))
        tag.id = cursor.getLong(CommentsTable.COLUMN_ID)
        val kidsString = cursor.getString(CommentsTable.COLUMN_KIDS)
        val count = if (kidsString.isNotEmpty()) {
            kidsString.split(',').size().toString()
        } else {
            ""
        }
        tag.kids.setText(count)

        val ancestorCount = cursor.getInt(CommentsTable.COLUMN_ANCESTOR_COUNT)

        if (ancestorCount < 9) {
            (0 .. 9).forEach {
                val colorView = tag.colorViews.get(it)
                if (it >= ancestorCount) colorView.setGone() else colorView.setVisible()
            }
        } else {
            (0 .. 8).forEach { tag.colorViews.get(it).setVisible() }
            val missingIndents = ancestorCount - 9
            val lastColorView = tag.colorViews.get(9)
            lastColorView.getLayoutParams().width = missingIndents.times(pxPadding.toInt())
        }
    }

    fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
    fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))
    fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))
    fun TextView.setHtmlText(source: String) = this.setText(Html.fromHtml(source))


    class ViewHolder(root: View) {
        val text = root.findViewById(R.id.text) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as TextView
        var id: Long? = null
        val kids = root.findViewById(R.id.kids) as TextView
        val colorViews = findColorViews(root as ViewGroup)

        fun findColorViews(root : ViewGroup): Array<View> =  Array(10, { root.getChildAt(it)})
    }

    val View.tag: ViewHolder
        get() {
            val tag = getTag()
            if (tag == null) {
                val newTag = ViewHolder(this)
                this.setTag(newTag)
                return newTag
            } else {
                return tag as ViewHolder
            }
        }

    fun max(i: Int, j: Int): Int = if (i > j) i else j

    fun View.setVisible() = this.setVisibility(View.VISIBLE)

    fun View.setGone() = this.setVisibility(View.GONE)
}



