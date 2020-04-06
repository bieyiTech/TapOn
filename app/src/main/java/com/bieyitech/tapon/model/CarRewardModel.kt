package com.bieyitech.tapon.model

import android.content.Context
import android.util.Log
import com.bieyitech.tapon.R
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.AnimationData
import com.google.ar.sceneform.rendering.ModelRenderable

class CarRewardModel : AbstractRewardModel() {

    override var canAnimate: Boolean = true

    override var renderable: ModelRenderable? = null

    override val localScale = Vector3(0.5f, 0.5f, 0.5f)
    override val localPosition: Vector3 = Vector3(0f, 0.02f, 0f)

    override fun loadRenderable(context: Context, exceptionally: (Throwable) -> Unit) {
        loadRenderableImpl(context, R.raw.car_anim, exceptionally)
    }

    override fun playAnimation() {
        super.playAnimation()
        try {
            val carAnimationData: AnimationData? = renderable?.getAnimationData("car_anim")
            val carAnimator = ModelAnimator(carAnimationData, renderable)

            with(carAnimator) {
                repeatCount = 5
                start()
            }
        } catch (e: ConcurrentModificationException) {
            Log.d("CouponRewardModel", e.message ?: "ConcurrentModificationException")
        }
    }

}