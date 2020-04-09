package com.bieyitech.tapon.update

import java.io.Serializable

// 版本信息，对应.json
data class UpdateVersionInfo(var versionCode: Int,
                             var versionName: String,
                             var apkUrl: String,
                             var updateInfo: String,
                             var date: String
): Serializable {
    companion object {
        private const val serialVersionUID = 20200408L
    }
}