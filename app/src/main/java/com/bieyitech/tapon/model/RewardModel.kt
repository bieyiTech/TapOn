package com.bieyitech.tapon.model

import android.content.Context
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable

// 奖品模型
interface RewardModel {

    // 是否带有动画
    var canAnimate: Boolean
    var renderable: ModelRenderable?
    val localScale: Vector3
    val localPosition: Vector3

    // 播放动画
    fun playAnimation()

    // 加载模型
    fun loadRenderable(context: Context, exceptionally: (Throwable) -> Unit)

}