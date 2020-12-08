package com.example.madam.utils

import java.nio.charset.Charset
import java.security.MessageDigest

class PasswordUtils {

    fun hash(pwd: String): String {
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        md.reset()
        md.update(pwd.toByteArray(Charset.forName("UTF-8")))
        val digest = md.digest(pwd.toByteArray(Charset.forName("UTF-8")))
        return bytesToHex(digest)
    }

    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (i in hash.indices) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}