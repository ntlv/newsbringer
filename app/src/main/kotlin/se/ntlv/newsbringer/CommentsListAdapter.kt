package se.ntlv.newsbringer

import android.database.Cursor
import android.content.Context
import android.widget.ResourceCursorAdapter
import android.view.View
import android.widget.TextView
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.NewsThreadListAdapter.ViewHolder
import se.ntlv.newsbringer.network.Metadata
import se.ntlv.newsbringer.database.CommentsTable
import android.text.Html
import android.graphics.Color
import android.util.TypedValue
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.ColorDrawable

open class CommentsListAdapter(ctx: Context, layout: Int, cursor: Cursor?, flags: Int) :
        ResourceCursorAdapter(ctx, layout, cursor, flags) {

    val pxPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 16f, ctx.getResources().getDisplayMetrics())

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val tag = view.tag
        tag.by.setText(cursor.getString(CommentsTable.COLUMN_BY))
        tag.time.setText(cursor.getString(CommentsTable.COLUMN_TIME))
        tag.text.setHtmlText(cursor.getString(CommentsTable.COLUMN_TEXT))
        tag.id = cursor.getLong(CommentsTable.COLUMN_ID)
        tag.kids.setText(cursor.getString(CommentsTable.COLUMN_KIDS))
        val padding = cursor.getInt(CommentsTable.COLUMN_ANCESTOR_COUNT).toFloat()

        view.setPadding((pxPadding + padding * 15f).toInt(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom())
        val minus = (246 - padding * 20).mod(256).toInt()
        val states = StateListDrawable();

        val pressed = IntArray(1)
        pressed.set(0, android.R.attr.state_pressed)

        val focused = IntArray(1)
        focused.set(0, android.R.attr.state_focused)

        states.addState(pressed, ColorDrawable(android.R.color.darker_gray));
        states.addState(focused, ColorDrawable(android.R.color.darker_gray));
        states.addState(IntArray(0), ColorDrawable(Color.rgb(246, minus, minus)));
        view.setBackground(states)
    }

    fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
    fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))
    fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))
    fun TextView.setHtmlText(source : String) = this.setText(Html.fromHtml(source))


    class ViewHolder(root: View) {
        val text = root.findViewById(R.id.text) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as TextView
        var id : Long? = null
        val kids = root.findViewById(R.id.kids) as TextView
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



}


