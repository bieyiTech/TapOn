package com.bieyitech.tapon.widgets

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.viewpager.widget.ViewPager
import com.giz.android.toolkit.dp2pxSize
import kotlin.math.abs


class CoverFlowEffectTransformer(context: Context) : ViewPager.PageTransformer {

    private var maxTranslateOffsetX = dp2pxSize(context, 180f)
    private var viewPager: ViewPager? = null

    override fun transformPage(view: View, position: Float) {
        if (viewPager == null) {
            viewPager = view.parent as ViewPager
        }
        val leftInScreen = view.left - viewPager!!.scrollX
        val centerXInViewPager = leftInScreen + view.measuredWidth / 2
        val offsetX = centerXInViewPager - viewPager!!.measuredWidth / 2
        val offsetRate =
            offsetX.toFloat() * 0.38f / viewPager!!.measuredWidth
        val scaleFactor = 1 - abs(offsetRate)
        if (scaleFactor > 0) {
            view.scaleX = scaleFactor
            view.scaleY = scaleFactor
            view.translationX = -maxTranslateOffsetX * offsetRate
            //ViewCompat.setElevation(view, 0.0f);
        }
        ViewCompat.setElevation(view, scaleFactor)
    }
}