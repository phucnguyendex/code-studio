package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GithubSearchResponse(
    @Json(name = "items") val items: List<GithubRepoItem>
)

@JsonClass(generateAdapter = true)
data class GithubRepoItem(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "description") val description: String?,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "stargazers_count") val stars: Int,
    @Json(name = "owner") val owner: GithubOwner
)

@JsonClass(generateAdapter = true)
data class GithubOwner(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String
)

@JsonClass(generateAdapter = true)
data class GithubContentItem(
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "type") val type: String, // "file" or "dir"
    @Json(name = "download_url") val downloadUrl: String?,
    @Json(name = "content") val content: String?,
    @Json(name = "encoding") val encoding: String?,
    @Json(name = "sha") val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class GithubCreateUpdateFileRequest(
    @Json(name = "message") val message: String,
    @Json(name = "content") val content: String, // base64 payload
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "branch") val branch: String? = null
)


@JsonClass(generateAdapter = true)
data class GithubGitRefResponse(
    @Json(name = "object") val obj: GithubGitRefObject
)
@JsonClass(generateAdapter = true)
data class GithubGitRefObject(
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GithubGitCommitResponse(
    @Json(name = "tree") val tree: GithubGitTreeObject
)
@JsonClass(generateAdapter = true)
data class GithubGitTreeObject(
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GithubCreateBlobRequest(
    @Json(name = "content") val content: String,
    @Json(name = "encoding") val encoding: String = "base64"
)
@JsonClass(generateAdapter = true)
data class GithubBlobResponse(
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GithubCreateTreeRequest(
    @Json(name = "base_tree") val baseTree: String,
    @Json(name = "tree") val tree: List<GithubTreeItemRequest>
)
@JsonClass(generateAdapter = true)
data class GithubTreeItemRequest(
    @Json(name = "path") val path: String,
    @Json(name = "mode") val mode: String = "100644",
    @Json(name = "type") val type: String = "blob",
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "content") val content: String? = null
)
@JsonClass(generateAdapter = true)
data class GithubTreeResponse(
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GithubCreateCommitRequest(
    @Json(name = "message") val message: String,
    @Json(name = "tree") val tree: String,
    @Json(name = "parents") val parents: List<String>
)
@JsonClass(generateAdapter = true)
data class GithubCreateCommitResponse(
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GithubUpdateRefRequest(
    @Json(name = "sha") val sha: String,
    @Json(name = "force") val force: Boolean = false
)

interface GithubApiService {
    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Header("Authorization") token: String
    ): GithubGitRefResponse

    @GET("repos/{owner}/{repo}/git/commits/{commit_sha}")
    suspend fun getCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("commit_sha") commitSha: String,
        @Header("Authorization") token: String
    ): GithubGitCommitResponse

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GithubCreateBlobRequest,
        @Header("Authorization") token: String
    ): GithubBlobResponse

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GithubCreateTreeRequest,
        @Header("Authorization") token: String
    ): GithubTreeResponse

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GithubCreateCommitRequest,
        @Header("Authorization") token: String
    ): GithubCreateCommitResponse

    @retrofit2.http.PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Body request: GithubUpdateRefRequest,
        @Header("Authorization") token: String
    ): okhttp3.ResponseBody

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
        @Header("Authorization") token: String? = null
    ): GithubSearchResponse

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String = "",
        @Header("Authorization") token: String? = null
    ): List<GithubContentItem>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Authorization") token: String? = null
    ): GithubContentItem

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: GithubCreateUpdateFileRequest,
        @Header("Authorization") token: String
    ): okhttp3.ResponseBody

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 15,
        @Header("Authorization") token: String? = null
    ): List<GithubCommitItem>

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 15,
        @Header("Authorization") token: String? = null
    ): List<GithubPullRequest>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepoDetail(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String? = null
    ): GithubRepoDetail
    @GET("repos/{owner}/{repo}/zipball/{branch}")
    suspend fun getZipArchive(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Header("Authorization") token: String? = null
    ): okhttp3.ResponseBody

    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String
    ): retrofit2.Response<Unit>

    @POST("repos/{owner}/{repo}/forks")
    suspend fun forkRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String
    ): GithubRepoItem

    @POST("user/repos")
    suspend fun createRepo(
        @Body request: GithubCreateRepoRequest,
        @Header("Authorization") token: String
    ): GithubRepoItem
}

@JsonClass(generateAdapter = true)
data class GithubCreateRepoRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "private") val isPrivate: Boolean = false,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GithubBranchInfo(
    @Json(name = "name") val name: String,
    @Json(name = "commit") val commit: GithubCommitPointer
)

@JsonClass(generateAdapter = true)
data class GithubCommitPointer(
    @Json(name = "sha") val sha: String,
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class GithubRepoDetail(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "description") val description: String?,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "stargazers_count") val stars: Int,
    @Json(name = "forks_count") val forks: Int,
    @Json(name = "open_issues_count") val openIssues: Int,
    @Json(name = "subscribers_count") val watchers: Int,
    @Json(name = "default_branch") val defaultBranch: String,
    @Json(name = "owner") val owner: GithubOwner
)

@JsonClass(generateAdapter = true)
data class GithubCommitItem(
    @Json(name = "sha") val sha: String,
    @Json(name = "commit") val commitDetail: GithubCommitDetail,
    @Json(name = "author") val authorInfo: GithubCommitAuthorInfo?
)

@JsonClass(generateAdapter = true)
data class GithubCommitDetail(
    @Json(name = "message") val message: String,
    @Json(name = "author") val author: GithubCommitAuthorDetail
)

@JsonClass(generateAdapter = true)
data class GithubCommitAuthorDetail(
    @Json(name = "name") val name: String,
    @Json(name = "date") val date: String
)

@JsonClass(generateAdapter = true)
data class GithubCommitAuthorInfo(
    @Json(name = "avatar_url") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class GithubPullRequest(
    @Json(name = "id") val id: Long,
    @Json(name = "number") val number: Int,
    @Json(name = "title") val title: String,
    @Json(name = "state") val state: String,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "user") val user: GithubOwner,
    @Json(name = "created_at") val createdAt: String
)

object GithubRetrofitClient {
    private const val BASE_URL = "https://api.github.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GithubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GithubApiService::class.java)
    }
}
