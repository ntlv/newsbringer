package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View

interface BViewHolder<in T>

abstract class BindingViewHolder<in T>(view: View) : ViewHolder(view) {
    abstract fun bind(item: T)
}
