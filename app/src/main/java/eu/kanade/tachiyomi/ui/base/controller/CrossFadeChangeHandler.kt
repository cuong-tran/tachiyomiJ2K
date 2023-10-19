package eu.kanade.tachiyomi.ui.base.controller

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler

class CrossFadeChangeHandler : AnimatorChangeHandler {
    constructor() : super()
    constructor(removesFromViewOnPush: Boolean) : super(removesFromViewOnPush)
    constructor(duration: Long) : super(duration)
    constructor(duration: Long, removesFromViewOnPush: Boolean) : super(
        duration,
        removesFromViewOnPush,
    )

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
    ): Animator {
        val animatorSet = AnimatorSet()
        if (to != null) {
            val start = if (toAddedToContainer) 0F else to.alpha
            animatorSet.play(ObjectAnimator.ofFloat(to, View.ALPHA, start, 1f))
        }
        if (from != null) {
            animatorSet.play(ObjectAnimator.ofFloat(from, View.ALPHA, 0f))
        }
        if (isPush) {
            if (from != null) {
                animatorSet.play(ObjectAnimator.ofFloat(from, View.TRANSLATION_X, -from.width.toFloat() * 0.2f))
            }
            if (to != null) {
                animatorSet.play(ObjectAnimator.ofFloat(to, View.TRANSLATION_X, to.width.toFloat() * 0.2f, 0f))
            }
        } else {
            if (from != null) {
                animatorSet.play(ObjectAnimator.ofFloat(from, View.TRANSLATION_X, from.width.toFloat() * 0.2f))
            }
            if (to != null) {
                // Allow this to have a nice transition when coming off an aborted push animation or
                // from back gesture
                val fromLeft = from?.translationX ?: 0F
                animatorSet.play(ObjectAnimator.ofFloat(to, View.TRANSLATION_X, fromLeft - to.width * 0.2f, 0f))
            }
        }
        return animatorSet
    }

    override fun resetFromView(from: View) {
        from.translationX = 0f
    }

    override fun copy(): ControllerChangeHandler =
        CrossFadeChangeHandler(animationDuration, removesFromViewOnPush)
}
