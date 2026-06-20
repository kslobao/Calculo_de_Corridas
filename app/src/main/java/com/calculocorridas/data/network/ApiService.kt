package com.calculocorridas.data.network

import com.calculocorridas.data.network.dto.DeviceRegisterRequest
import com.calculocorridas.data.network.dto.DeviceRegisterResponse
import com.calculocorridas.data.network.dto.LicenseCheckRequest
import com.calculocorridas.data.network.dto.LicenseResponseDto
import com.calculocorridas.data.network.dto.ParserReportRequest
import com.calculocorridas.data.network.dto.SelectorConfigDto
import com.calculocorridas.data.network.dto.SubscriptionValidateRequest
import com.calculocorridas.data.network.dto.SubscriptionValidateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/v1/device/register")
    suspend fun registerDevice(
        @Body request: DeviceRegisterRequest
    ): Response<DeviceRegisterResponse>

    @GET("api/v1/selectors")
    suspend fun getSelectors(
        @Query("version") localVersion: Int
    ): Response<SelectorConfigDto>

    @POST("api/v1/license/check")
    suspend fun checkLicense(
        @Body request: LicenseCheckRequest
    ): Response<LicenseResponseDto>

    @POST("api/v1/subscription/validate")
    suspend fun validateSubscription(
        @Body request: SubscriptionValidateRequest
    ): Response<SubscriptionValidateResponse>

    @POST("api/v1/parser/report")
    suspend fun reportParserFailure(
        @Body request: ParserReportRequest
    ): Response<Unit>
}
