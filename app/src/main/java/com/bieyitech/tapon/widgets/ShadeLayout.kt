package com.bieyitech.tapon.widgets

import android.content.Context
import android.content.res.TypedArray
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.widget.FrameLayout
import com.bieyitech.tapon.R
import kotlin.math.min

class ShadeLayout(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    // 自定义属性
    private var cornerRadius = 0f // 圆角半径
    private var shadeLength = 16f
    private var shadeColor = Color.LTGRAY
    private var mSolidColor = Color.WHITE
    private var yOffset = 0f

    // 内部变量
    private val mSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mShadePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mMaskPaint = Paint().also { it.color = Color.argb(48,0, 0, 0) }

    private var extraShadeLength = dp2px(1f)

    var showMask = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        val array: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.ShadeLayout, defStyleAttr, 0)

        cornerRadius = array.getDimension(R.styleable.ShadeLayout_sl_radius, dp2px(8f))
        shadeLength = array.getDimension(R.styleable.ShadeLayout_sl_shadeLength, dp2px(8f))
        shadeColor = array.getColor(R.styleable.ShadeLayout_sl_shadeColor, Color.LTGRAY)
        mSolidColor = array.getColor(R.styleable.ShadeLayout_sl_solidColor, Color.WHITE)
        yOffset = array.getDimension(R.styleable.ShadeLayout_sl_yOffset, 0f)

        array.recycle()

        extraShadeLength += yOffset / 2f

        // 为阴影腾出空间
        val padding = shadeLength.toInt()
        setPadding(padding + paddingStart, padding + paddingTop,
            padding + paddingRight, padding + paddingBottom)
    }

    init {
        setWillNotDraw(false) // 这样才能调用onDraw函数
        with(mShadePaint){
            color = shadeColor
            if(yOffset > 0f){
                maskFilter = BlurMaskFilter(shadeLength, BlurMaskFilter.Blur.NORMAL)
            }else{
                maskFilter = BlurMaskFilter(shadeLength, BlurMaskFilter.Blur.OUTER)
            }
        }
        mSolidPaint.color = mSolidColor
    }

    private fun dp2px(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value,
            resources.displayMetrics
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 判断阴影长度
        val maxL = min(height, width)
        if((shadeLength + extraShadeLength) * 2 > maxL) {
            shadeLength = maxL / 4f
        }
        // 保证圆角最大为实心高度的一半
        val radius = height / 2f - shadeLength - extraShadeLength
        cornerRadius = min(cornerRadius, radius)
    }

    override fun onDraw(canvas: Canvas) {
        val sl = shadeLength + extraShadeLength
        // 绘制阴影
        if(yOffset > 0){
            canvas.drawRoundRect(sl, sl + yOffset, width - sl, height - sl + yOffset,
                cornerRadius, cornerRadius, mShadePaint)
        }else{
            canvas.drawRoundRect(sl, sl, width - sl, height - sl,
                cornerRadius, cornerRadius, mShadePaint)
        }
        // 绘制实心部分
        val sll = sl - 2f
        canvas.drawRoundRect(sll, sll, width - sll, height - sll,
            cornerRadius, cornerRadius, mSolidPaint)
        // 绘制遮罩
        if(showMask) {
            canvas.drawRoundRect(sll, sll, width - sll, height - sll,
                cornerRadius, cornerRadius, mMaskPaint)
        }
    }

    /**
     * 设置圆角半径
     * @param radius 半径值，单位dp
     */
    fun setCornerRadius(radius: Float) {
        cornerRadius = dp2px(radius)
        invalidate()
    }

}