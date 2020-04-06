package com.bieyitech.tapon.model

import android.content.Context
import android.util.Log
import com.bieyitech.tapon.R
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable

class CouponRewardModel : AbstractRewardModel() {
    override var canAnimate: Boolean = true

    override var renderable: ModelRenderable? = null

    override val localScale: Vector3 = Vector3(0.6f, 0.6f, 0.6f)
    override val localPosition: Vector3 = Vector3(0f, 0.02f, 0f)

    override fun playAnimation() {
        super.playAnimation()
        try {
            val couponAnimationData = renderable?.getAnimationData("coupon_anim")
            val couponAnimator = ModelAnimator(couponAnimationData, renderable)

            with(couponAnimator) {
                repeatCount = 5
                start()
            }
        } catch (e: ConcurrentModificationException) {
            Log.d("CouponRewardModel", e.message ?: "ConcurrentModificationException")
        }
    }

    override fun loadRenderable(context: Context, exceptionally: (Throwable) -> Unit) {
        loadRenderableImpl(context, R.raw.coupon_anim, exceptionally)
    }
}