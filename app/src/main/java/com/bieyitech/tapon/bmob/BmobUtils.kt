package com.bieyitech.tapon.bmob

import cn.bmob.v3.datatype.BmobDate
import java.util.*

// 应用ID
const val APPLICATION_ID = "1336bb2d340f2ea22dc25d0e2e16cda9"

fun BmobDate.fetchDate(): Date = Date(BmobDate.getTimeStamp(date))