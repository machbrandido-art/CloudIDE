package com.example.cloudide.data.github

import com.example.cloudide.data.github.dto.DispatchRequestDto
import com.example.cloudide.data.github.dto.PutContentRequest
import com.example.cloudide.data.github.dto.RepoDto
import com.example.cloudide.data.github.dto.UserDto
import com.example.cloudide.data.github.dto.WorkflowJobsResponseDto
import com.example.cloudide.data.github.dto.WorkflowRunDto
import com.example.cloudide.data.github.dto.WorkflowRunsDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApi {

    @GET("user")
    suspend fun getUser(): UserDto

    @GET("user")
    suspend fun getUserWithToken(@Header("Authorization") authorization: String): UserDto

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

    // ---------- Stage 4: Build engine ----------

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body body: DispatchRequestDto,
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getRunsByEvent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("event") event: String,
        @Query("created") created: String,
        @Query("per_page") perPage: Int = 10,
    ): Response<WorkflowRunsDto>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
    suspend fun getRunById(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
    ): Response<WorkflowRunDto>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
    suspend fun getJobsForRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
    ): Response<WorkflowJobsResponseDto>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/cancel")
    suspend fun cancelRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/rerun")
    suspend fun rerunRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null,
    ): Response<ResponseBody>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: PutContentRequest,
    ): Response<ResponseBody>
}