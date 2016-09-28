package se.ntlv.newsbringer.customviews

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.annotation.ColorInt
import android.support.v7.widget.RecyclerView
import android.view.View
import org.jetbrains.anko.forEachChild

class OnePixelSeparator(@ColorInt color: Int) : RecyclerView.ItemDecoration() {
    val paint: Paint
    val target = Rect()

    init {
        paint = Paint()
        paint.color = color
        paint.alpha = 64
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (state.willRunPredictiveAnimations() || state.willRunSimpleAnimations() || state.isMeasuring || state.isPreLayout) {
            return
        }

        val dividerLeft = parent.paddingLeft
        val dividerRight = parent.width - parent.paddingRight

        parent.forEachChild {
            val params = it.layoutParams as RecyclerView.LayoutParams
            val dividerTop = it.bottom + params.bottomMargin
            val dividerBottom = dividerTop + 1

            target.set(dividerLeft, dividerTop, dividerRight, dividerBottom)

            c.drawRect(target, paint)
        }
    }

    override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent?.getChildAdapterPosition(view) == 0) {
            return
        }
        outRect?.top = 1

    }
}
