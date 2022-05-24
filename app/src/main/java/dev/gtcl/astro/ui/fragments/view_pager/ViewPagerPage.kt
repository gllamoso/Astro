package dev.gtcl.astro.ui.fragments.view_pager

import android.os.Parcelable
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.models.reddit.listing.PostListing
import kotlinx.android.parcel.Parcelize

sealed class ViewPagerPage : Parcelable

@Parcelize
class ListingPage(
    val postListing: PostListing
) : ViewPagerPage()

@Parcelize
class AccountPage(
    val user: String?
) : ViewPagerPage()

@Parcelize
class PostPage(
    val post: Post,
    val position: Int
) : ViewPagerPage()

@Parcelize
class CommentsPage(
    val url: String,
    val expandReplies: Boolean
) : ViewPagerPage()

@Parcelize
object InboxPage : ViewPagerPage()