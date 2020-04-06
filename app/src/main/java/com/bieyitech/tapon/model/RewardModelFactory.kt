package com.bieyitech.tapon.model

import com.bieyitech.tapon.R

object RewardModelFactory {

    fun getRewardModel(imgCode: Int): RewardModel = when(imgCode) {
        0 -> CarRewardModel()
        1 -> FuwaRewardModel()
        2 -> CouponRewardModel()
        else -> CouponRewardModel()
    }

    fun getRewardImgList() = arrayOf(R.raw.car, R.raw.fuwa, R.raw.coupon)

    fun getRewardImg(imgCode: Int) = when(imgCode) {
        0 -> R.raw.car
        1 -> R.raw.fuwa
        2 -> R.raw.coupon
        else -> R.drawable.coupon_example
    }

}