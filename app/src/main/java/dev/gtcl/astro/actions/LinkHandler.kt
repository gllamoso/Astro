package dev.gtcl.astro.actions

import dev.gtcl.astro.url.URL

interface LinkHandler {
    fun handleLink(url: URL)
}