package com.calculocorridas.data.network

import com.calculocorridas.data.network.dto.LicenseCheckRequest
import com.calculocorridas.data.network.dto.LicenseResponseDto
import com.calculocorridas.data.network.dto.ParserFailureRequest
import com.calculocorridas.data.network.dto.SelectorConfigDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("api/v1/selectors")
    suspend fun getSelectors(
        @Query("version") localVersion: Int
    ): Response<SelectorConfigDto>

    @POST("api/v1/license/check")
    suspend fun checkLicense(
        @Body request: LicenseCheckRequest
    ): Response<LicenseResponseDto>

    @POST("api/v1/parser/report")
    suspend fun reportParserFailure(
        @Body request: ParserFailureRequest
    ): Response<Unit>
}
