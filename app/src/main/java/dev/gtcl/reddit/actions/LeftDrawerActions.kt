package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.Account


interface LeftDrawerActions{
    // Accounts
    fun onAddAccountClicked()
    fun onRemoveAccountClicked(username: String)
    fun onAccountClicked(account: Account)
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