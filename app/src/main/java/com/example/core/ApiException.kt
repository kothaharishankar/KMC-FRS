package com.example.core

/**
 * A shared ApiException class every remote service throws on failure,
 * allowing UI layers to show precise, descriptive error messages instead of generic ones.
 */
class ApiException(message: String, val code: Int? = null) : Exception(message)
