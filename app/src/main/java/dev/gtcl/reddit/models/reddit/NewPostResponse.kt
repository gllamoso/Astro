package dev.gtcl.reddit.models.reddit

data class NewPostResponse(val json: NewPostJson)
data class NewPostJson(val data: NewPostData)
data class NewPostData(
    val url: String,
    val id: String,
    val name: String
)