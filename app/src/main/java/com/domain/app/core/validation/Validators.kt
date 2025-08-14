package com.domain.app.core.validation

/**
 * Type alias for validator functions
 */
typealias Validator<T> = (T) -> ValidationResult

/**
 * Common validators used throughout the application
 */
object Validators {
    /**
     * Validates that a string is not null or blank
     */
    fun required(fieldName: String = "Field"): Validator<String?> = { value ->
        if (value.isNullOrBlank()) 
            ValidationResult.Error("$fieldName is required")
        else 
            ValidationResult.Success
    }
    
    /**
     * Validates that a number is within a range
     */
    fun numberRange(min: Number, max: Number, fieldName: String = "Value"): Validator<Number?> = { value ->
        when {
            value == null -> ValidationResult.Error("$fieldName is required")
            value.toDouble() < min.toDouble() -> ValidationResult.Error("$fieldName must be at least $min")
            value.toDouble() > max.toDouble() -> ValidationResult.Error("$fieldName must be at most $max")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates that a number is positive
     */
    fun positive(fieldName: String = "Value"): Validator<Number?> = { value ->
        when {
            value == null -> ValidationResult.Error("$fieldName is required")
            value.toDouble() <= 0 -> ValidationResult.Error("$fieldName must be positive")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates email format
     */
    fun email(): Validator<String?> = { value ->
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        when {
            value.isNullOrBlank() -> ValidationResult.Error("Email is required")
            !value.matches(emailRegex) -> ValidationResult.Error("Invalid email format")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates minimum length
     */
    fun minLength(min: Int, fieldName: String = "Field"): Validator<String?> = { value ->
        when {
            value == null -> ValidationResult.Error("$fieldName is required")
            value.length < min -> ValidationResult.Error("$fieldName must be at least $min characters")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates maximum length
     */
    fun maxLength(max: Int, fieldName: String = "Field"): Validator<String?> = { value ->
        when {
            value == null -> ValidationResult.Success // Max length doesn't require value
            value.length > max -> ValidationResult.Error("$fieldName must be at most $max characters")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Combines multiple validators
     */
    fun <T> combine(vararg validators: Validator<T>): Validator<T> = { value ->
        validators.asSequence()
            .map { it(value) }
            .firstOrNull { it !is ValidationResult.Success }
            ?: ValidationResult.Success
    }
    
    /**
     * Creates a warning if a condition is met
     */
    fun <T> warnIf(condition: (T) -> Boolean, message: String): Validator<T> = { value ->
        if (condition(value)) 
            ValidationResult.Warning(message)
        else 
            ValidationResult.Success
    }
}
