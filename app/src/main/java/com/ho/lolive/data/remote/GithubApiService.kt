package com.ho.lolive.data.remote

import com.ho.lolive.data.remote.dto.GithubReleaseResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface GithubApiService {
    @GET
    suspend fun getLatestRelease(@Url url: String): GithubReleaseResponse
}
