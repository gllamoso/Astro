package dev.gtcl.astro.models.imgur

data class ImgurResponse(
    val status: Int,
    val success: Boolean,
    val data: ImgurData
)