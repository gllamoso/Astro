package dev.gtcl.astro.ui.fragments.create_post

import android.net.Uri


sealed class PostContent
class TextPost(val body: String) : PostContent()
class ImagePost(val uri: Uri) : PostContent()
class LinkPost(val url: String) : PostContent()