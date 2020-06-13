package dev.gtcl.reddit.actions

import dev.gtcl.reddit.database.SavedAccount


interface LeftDrawerActions{
    // Accounts
    fun onAddAccountClicked()
    fun onRemoveAccountClicked(user: String)
    fun onAccountClicked(account: SavedAccount)
    fun onLogoutClicked()

    // Posts
    fun onHomeClicked()

    // My Account
    fun onMyAccountClicked()

    // Inbox
    fun onInboxClicked()

    // Settings
    fun onSettingsClicked()
}