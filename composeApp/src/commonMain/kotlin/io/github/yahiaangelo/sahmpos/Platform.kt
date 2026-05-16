package io.github.yahiaangelo.sahmpos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform