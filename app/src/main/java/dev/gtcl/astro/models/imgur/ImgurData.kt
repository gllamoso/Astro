package dev.gtcl.astro.models.imgur

data class ImgurData(
    val id: String,
    val link: String,
    val type: String?,
    val images: List<ImgurImage>?
)