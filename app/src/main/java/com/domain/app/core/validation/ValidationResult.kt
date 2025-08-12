// app/src/main/java/com/domain/app/core/validation/ValidationResult.kt
package com.domain.app.core.validation

/**
 * Unified validation result used across the entire application.
 * This replaces duplicate definitions previously in Plugin.kt and ValidatedTextField.kt
 * 
 * Usage:
 * - Success: Indicates validation passed
 * - Error: Indicates validation failed with a required fix
 * - Warning: Indicates validation passed but with a cautionary message
 */
sealed class ValidationResult {
    /**
     * Validation succeeded without any issues
     */
    object Success : ValidationResult()
    
    /**
     * Validation failed with an error that must be fixed
     * @param message The error message to display to the user
     * @param field Optional field name for field-specific errors
     */
    data class Error(
        val message: String,
        val field: String? = null
    ) : ValidationResult()
    
    /**
     * Validation succeeded but with a warning
     * @param message The warning message to display to the user
     * @param field Optional field name for field-specific warnings
     */
    data class Warning(
        val message: String,
        val field: String? = null
    ) : ValidationResult()
    
    /**
     * Helper methods for creating validation results
     */
    companion object {
        /**
         * Create a success result
         */
        fun success() = Success
        
        /**
         * Create an error result with a message
         * @param message The error message
         * @param field Optional field identifier
         */
        fun error(message: String, field: String? = null) = Error(message, field)
        
        /**
         * Create a warning result with a message
         * @param message The warning message
         * @param field Optional field identifier
         */
        fun warning(message: String, field: String? = null) = Warning(message, field)
    }
    
    /**
     * Check if the validation was successful (Success or Warning)
     */
    fun isValid(): Boolean = this is Success || this is Warning
    
    /**
     * Check if there's an error
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Check if there's a warning
     */
    fun hasWarning(): Boolean = this is Warning
    
    /**
     * Get the message if any (error or warning)
     */
    fun getMessage(): String? = when (this) {
        is Success -> null
        is Error -> message
        is Warning -> message
    }
}
