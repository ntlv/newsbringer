package se.ntlv.newsbringer.customviews

import android.view.MenuItem
import android.widget.ImageView
import org.jetbrains.anko.AnkoLogger

class RefreshButtonAnimator(private val button: MenuItem, private val actionView: ImageView) : AnkoLogger {
    private val mDeferredAnimationDefault: () -> Unit = { }
    private var mDeferredAnimation: () -> Unit = mDeferredAnimationDefault
    private var mIsRunningStartLoadingAnimation = false

    init {
        button.actionView = actionView
    }

    private var mIsIndicatingLoading = false

    fun indicateLoading(isLoading: Boolean) {
        if (mIsIndicatingLoading == isLoading) return

        mIsIndicatingLoading = isLoading

        if (isLoading) {
            mIsRunningStartLoadingAnimation = true
            button.isEnabled = false

            actionView.animate().setDuration(500).rotation(180f).alpha(0.25f).withEndAction {
                mIsRunningStartLoadingAnimation = false
                mDeferredAnimation()
            }
        } else {

            val stopAnimation: () -> Unit = {
                actionView.animate().setDuration(500).setStartDelay(500).rotation(0f).alpha(1.0f).withEndAction {
                    button.isEnabled = true
                    mDeferredAnimation = mDeferredAnimationDefault
                }
            }
            if (mIsRunningStartLoadingAnimation) {
                mDeferredAnimation = stopAnimation
            } else {
                stopAnimation()
            }
        }
    }
}
