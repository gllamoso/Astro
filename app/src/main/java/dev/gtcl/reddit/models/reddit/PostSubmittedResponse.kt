package dev.gtcl.reddit.models.reddit

data class PostSubmittedResponse(val json: PostSubmittedJson)
data class PostSubmittedJson(val data: PostSubmittedData)
data class PostSubmittedData(
    val url: String,
    val id: String,
    val name: String
)