package com.example.cloudide.data.github

import com.example.cloudide.data.github.dto.RepoDto
import com.example.cloudide.data.github.dto.UserDto
import com.example.cloudide.data.github.dto.WorkflowRunsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApi {

    /** Uses the stored token (added by AuthInterceptor). */
    @GET("user")
    suspend fun getUser(): UserDto

    /**
     * Validates a token that is not stored yet. Because the Authorization header is set
     * explicitly, AuthInterceptor leaves it alone and a 401 does not clear the session.
     */
    @GET("user")
    suspend fun getUserWithToken(@Header("Authorization") authorization: String): UserDto

    /** Returns Response so we can read the ETag, the Link header and handle 304. */
    @GET("user/repos")
    suspend fun getUserRepos(
        @Query("sort") sort: String,
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
        @Header("If-None-Match") etag: String?
    ): Response<List<RepoDto>>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getLatestRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int,
        @Header("If-None-Match") etag: String?
    ): Response<WorkflowRunsDto>
}