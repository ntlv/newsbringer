package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView
import android.view.View

abstract class BindingViewHolder<in T>( view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(item : T)
}
