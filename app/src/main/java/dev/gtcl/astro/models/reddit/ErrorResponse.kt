package dev.gtcl.astro.models.reddit

data class ErrorResponse(val json: Error)
data class Error(val errors: List<List<String>>)