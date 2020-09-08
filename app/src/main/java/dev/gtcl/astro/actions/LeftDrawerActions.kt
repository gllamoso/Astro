package dev.gtcl.astro.actions

import dev.gtcl.astro.database.SavedAccount


interface LeftDrawerActions {
    // Accounts
    fun onAddAccountClicked()
    fun onRemoveAccountClicked(account: SavedAccount)
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