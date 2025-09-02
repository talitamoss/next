// app/src/main/java/com/domain/app/core/encryption/EncryptionManager.kt
package com.domain.app.core.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encryption and decryption of sensitive data using AES-256
 */
@Singleton
class EncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "AppDataEncryptionKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    init {
        // Generate encryption key if it doesn't exist
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }
    
    /**
     * Generate a new AES-256 encryption key in the Android Keystore
     */
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Set to true for biometric protection
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    /**
     * Get the encryption key from the Keystore
     */
    private fun getKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
    
    /**
     * Encrypt a string using AES-256-GCM
     * @param plainText The text to encrypt
     * @return Base64 encoded encrypted string with IV prepended
     */
    fun encryptString(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV and cipher text
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }
    
    /**
     * Decrypt a string encrypted with encryptString
     * @param encryptedText Base64 encoded encrypted string with IV prepended
     * @return Decrypted plain text
     */
    fun decryptString(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.DEFAULT)
        
        // Extract IV and cipher text
        val iv = ByteArray(IV_LENGTH)
        val cipherText = ByteArray(combined.size - IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
        System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        
        val plainText = cipher.doFinal(cipherText)
        return String(plainText, Charsets.UTF_8)
    }
    
    /**
     * Encrypt a byte array
     * @param data The data to encrypt
     * @return Encrypted byte array with IV prepended
     */
    fun encryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        
        val iv = cipher.iv
        val cipherText = cipher.doFinal(data)
        
        // Combine IV and cipher text
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        
        return combined
    }
    
    /**
     * Decrypt a byte array encrypted with encryptData
     * @param encryptedData Encrypted byte array with IV prepended
     * @return Decrypted data
     */
    fun decryptData(encryptedData: ByteArray): ByteArray {
        // Extract IV and cipher text
        val iv = ByteArray(IV_LENGTH)
        val cipherText = ByteArray(encryptedData.size - IV_LENGTH)
        System.arraycopy(encryptedData, 0, iv, 0, IV_LENGTH)
        System.arraycopy(encryptedData, IV_LENGTH, cipherText, 0, cipherText.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        
        return cipher.doFinal(cipherText)
    }
    
    /**
     * Check if encryption is available and properly configured
     */
    fun isEncryptionAvailable(): Boolean {
        return try {
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete the encryption key (use with caution - will make encrypted data unreadable)
     */
    fun deleteEncryptionKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
    
    /**
     * Re-generate the encryption key (will make old encrypted data unreadable)
     */
    fun regenerateKey() {
        deleteEncryptionKey()
        generateKey()
    }
}
