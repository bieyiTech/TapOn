package com.bieyitech.tapon.helpers

import java.util.regex.Pattern

// 字符串相关工具函数

fun String?.isUrl(loose: Boolean = false) : Boolean {
    val expr = "(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]"
    val looseExpr = "[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]"
    return when (loose) {
        true -> (this?.matches(Regex(expr)) ?: false) || (this?.matches(Regex(looseExpr)) ?: false)
        false -> this?.matches(Regex(expr)) ?: false
    }
}

fun String.isEmail(): Boolean =
    Pattern.matches("^([a-z0-9A-Z]+[-|\\\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$",
        this)