package com.example.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    fun encrypt(plainText: String, passcode: String): String {
        // Derive key from passcode using SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passcode.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // Deterministic IV to make password verification possible during decryption without storing salt
        val ivBytes = digest.digest((passcode + "PERSONAL_FINANCE_SALT_IV").toByteArray(Charsets.UTF_8)).copyOf(16)
        val ivParameterSpec = IvParameterSpec(ivBytes)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String, passcode: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passcode.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val ivBytes = digest.digest((passcode + "PERSONAL_FINANCE_SALT_IV").toByteArray(Charsets.UTF_8)).copyOf(16)
        val ivParameterSpec = IvParameterSpec(ivBytes)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
        val encryptedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
