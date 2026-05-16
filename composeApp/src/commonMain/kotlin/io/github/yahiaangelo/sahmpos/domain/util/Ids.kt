package io.github.yahiaangelo.sahmpos.domain.util

import kotlin.random.Random

object Ids {
    private val chars = "0123456789abcdef"

    fun newId(prefix: String = "id"): String {
        val sb = StringBuilder(prefix.length + 17)
        sb.append(prefix).append('_')
        repeat(16) { sb.append(chars[Random.nextInt(16)]) }
        return sb.toString()
    }
}