package com.bieyitech.tapon.model

import android.content.Context
import android.util.Log
import com.bieyitech.tapon.R
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.AnimationData
import com.google.ar.sceneform.rendering.ModelRenderable

class FuwaRewardModel : AbstractRewardModel() {

    override var canAnimate: Boolean = true

    override var renderable: ModelRenderable? = null

    override val localScale = Vector3(0.5f, 0.5f, 0.5f)
    override val localPosition: Vector3 = Vector3.zero()

    override fun loadRenderable(context: Context, exceptionally: (Throwable) -> Unit) {
        loadRenderableImpl(context, R.raw.fuwa_anim, exceptionally)
    }

    override fun playAnimation() {
        super.playAnimation()
        try {
            val fuwaAnimationData: AnimationData? = renderable?.getAnimationData("fuwa_anim")
            val fuwaAnimator = ModelAnimator(fuwaAnimationData, renderable)

            with(fuwaAnimator) {
                repeatCount = 5
                duration = 1500
                start()
            }
        } catch (e: ConcurrentModificationException) {
            Log.d("CouponRewardModel", e.message ?: "ConcurrentModificationException")
        }
    }

}