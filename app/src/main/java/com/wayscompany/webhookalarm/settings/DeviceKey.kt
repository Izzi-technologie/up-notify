package com.wayscompany.webhookalarm.settings

import java.security.SecureRandom
import java.util.Random

object DeviceKey {
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 5

    private val pattern = Regex("^[$ALPHABET]{$LENGTH}$")

    fun isValid(value: String): Boolean = pattern.matches(value.trim())

    fun generate(random: Random = SecureRandom()): String =
        CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] }.concatToString()

    fun resolve(current: String, random: Random = SecureRandom()): String {
        val trimmed = current.trim()
        return if (isValid(trimmed)) trimmed else generate(random)
    }
}
