package dev.gtcl.reddit.ui.fragments.account.pages

import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.models.reddit.ProfileListing

class UserPostsFragment : SimpleListingScrollerFragment() {

    override fun setListingInfo() {
        model.setListingInfo(ProfileListing(ProfileInfo.SUBMITTED), PostSort.BEST, null, 40)
    }

}