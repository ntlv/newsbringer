package se.ntlv.newsbringer.database

import android.support.v7.util.DiffUtil

class CompositeCursor<out T : Identifiable>(val header: T, val comments: TypedCursor<T>) : TypedCursor<T> by comments {

    override var diff: DiffUtil.DiffResult? = null

    override fun get(position: Int): T = when (position) {
        0 -> header
        else -> {
            comments[position - 1]
        }
    }

    override fun getCount(): Int {
        return comments.count + 1
    }
}
