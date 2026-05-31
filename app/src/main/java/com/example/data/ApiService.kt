package com.example.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SotarApiService {

    @GET("apps")
    suspend fun getApps(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<AppsListResponse>

    @GET("apps/{id}")
    suspend fun getApp(@Path("id") id: Long): Response<AppResponse>

    @Multipart
    @POST("apps")
    suspend fun publishApp(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("version") version: RequestBody,
        @Part("developer_name") developerName: RequestBody,
        @Part("developer_email") developerEmail: RequestBody,
        @Part("developer_bio") developerBio: RequestBody,
        @Part("icon_color") iconColor: RequestBody,
        @Part("icon_symbol") iconSymbol: RequestBody,
        @Part apk: MultipartBody.Part? = null
    ): Response<AppResponse>

    @DELETE("apps/{id}")
    suspend fun deleteApp(@Path("id") id: Long): Response<SuccessResponse>

    @GET("apps/{id}/reviews")
    suspend fun getReviews(@Path("id") appId: Long): Response<ReviewsListResponse>

    @FormUrlEncoded
    @POST("apps/{id}/reviews")
    suspend fun addReview(
        @Path("id") appId: Long,
        @Field("reviewer_name") reviewerName: String,
        @Field("reviewer_email") reviewerEmail: String,
        @Field("text") text: String,
        @Field("rating") rating: Int
    ): Response<SuccessResponse>

    @GET("developers/{id}")
    suspend fun getDeveloper(@Path("id") id: Long): Response<DeveloperResponse>

    @GET("categories")
    suspend fun getCategories(): Response<CategoriesResponse>
}
