package se.ntlv.newsbringer.customviews

import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.ViewCompat
import android.view.View


class AppBarLayoutDependencyBehaviour : CoordinatorLayout.Behavior<FloatingActionButton>() {

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout?, child: FloatingActionButton?, directTargetChild: View?, target: View?, nestedScrollAxes: Int): Boolean {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target,
                        nestedScrollAxes)
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout?, child: FloatingActionButton?, target: View?, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)

        if (dyConsumed > 0 && child?.visibility == View.VISIBLE) {
            child?.hide()
        } else if (dyConsumed < 0 && child?.visibility != View.VISIBLE) {
            child?.show()
        }
    }
}

fun FloatingActionButton?.applyAppBarLayoutDependency() {
    if (this != null) {
        val p = this.layoutParams as (CoordinatorLayout.LayoutParams)
        p.behavior = AppBarLayoutDependencyBehaviour()
        this.layoutParams = p
    }
}
