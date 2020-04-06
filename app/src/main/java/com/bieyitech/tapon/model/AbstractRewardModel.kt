package com.bieyitech.tapon.model

import android.content.Context
import com.bieyitech.tapon.R
import com.google.ar.sceneform.rendering.ModelRenderable

abstract class AbstractRewardModel : RewardModel{

    override fun playAnimation() {
        canAnimate = false
    }

    protected fun loadRenderableImpl(context: Context, resId: Int, exceptionally: (Throwable) -> Unit = {}) {
        ModelRenderable.builder()
            .setSource(context, resId)
            .build()
            .thenAccept {
                renderable = it
            }
            .exceptionally {
                exceptionally(it)
                null
            }
    }


}