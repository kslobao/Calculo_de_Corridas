package com.calculocorridas.data.network.dto

import com.google.gson.annotations.SerializedName

data class DeviceRegisterRequest(
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_version")  val appVersion: String
)

data class DeviceRegisterResponse(
    @SerializedName("registered") val registered: Boolean,
    @SerializedName("device_id")  val deviceId: String,
    @SerializedName("plan")       val plan: String
)
