package se.ntlv.newsbringer.comments

import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.*

class FabManager(private val fab: FloatingActionButton) : OnScrollListener() {

    private var didScrollUp = false

    override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) =
            when (didScrollUp && (newState == SCROLL_STATE_IDLE || newState == SCROLL_STATE_SETTLING)) {
                true -> fab.show()
                false -> fab.hide()
            }

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        didScrollUp = dy < 0
    }
}