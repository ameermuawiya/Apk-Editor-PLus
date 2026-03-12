package com.saas.apkeditorplus

import com.google.gson.annotations.SerializedName

data class GitHubCommit(
    @SerializedName("sha") val sha: String,
    @SerializedName("commit") val commitDetails: CommitDetails,
    @SerializedName("author") val author: CommitAuthor?
)

data class CommitDetails(
    @SerializedName("author") val author: AuthorInfo,
    @SerializedName("message") val message: String
)

data class AuthorInfo(
    @SerializedName("name") val name: String,
    @SerializedName("date") val date: String
)

data class CommitAuthor(
    @SerializedName("avatar_url") val avatarUrl: String
)
