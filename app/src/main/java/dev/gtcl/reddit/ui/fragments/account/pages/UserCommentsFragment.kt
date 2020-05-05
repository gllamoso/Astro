package dev.gtcl.reddit.ui.fragments.account.pages

import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.models.reddit.ProfileListing

class UserCommentsFragment : SimpleListingScrollerFragment() {
    override fun setListingInfo() {
        model.setListingInfo(ProfileListing(ProfileInfo.COMMENTS), PostSort.BEST, null, 40)
    }

}