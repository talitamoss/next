package com.domain.app.ui.components.core.input

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.domain.app.ui.theme.AppIcons
import com.domain.app.core.validation.ValidationResult
import com.domain.app.core.validation.Validator

/**
 * A text field with built-in validation, error handling, and formatting.
 * Replaces all the duplicate text field implementations across forms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    validator: ((String) -> ValidationResult)? = null,
    formatter: ((String) -> String)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLength: Int? = null,
    showCharacterCount: Boolean = false,
    required: Boolean = false,
    validateOnFocusLost: Boolean = true
) {
    var internalValue by remember(value) { mutableStateOf(value) }
    var validationResult by remember { mutableStateOf<ValidationResult>(ValidationResult.Success) }
    var hasFocus by remember { mutableStateOf(false) }
    var hasBeenFocused by remember { mutableStateOf(false) }
    
    // Validate on value change
    LaunchedEffect(value) {
        if (validator != null && (hasBeenFocused || value.isNotEmpty())) {
            validationResult = validator(value)
        }
    }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = internalValue,
            onValueChange = { newValue ->
                val processedValue = when {
                    maxLength != null && newValue.length > maxLength -> newValue.take(maxLength)
                    formatter != null -> formatter(newValue)
                    else -> newValue
                }
                internalValue = processedValue
                onValueChange(processedValue)
                
                // Immediate validation for certain cases
                if (validator != null && (hasBeenFocused || processedValue.isNotEmpty())) {
                    validationResult = validator(processedValue)
                }
            },
            label = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label)
                    if (required) {
                        Text(
                            text = "*",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                Row {
                    // Validation status icon
                    AnimatedVisibility(
                        visible = hasBeenFocused && !hasFocus,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        when (validationResult) {
                            is ValidationResult.Success -> {
                                if (value.isNotEmpty()) {
                                    Icon(
                                        imageVector = AppIcons.Status.success,
                                        contentDescription = "Valid",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            is ValidationResult.Error -> {
                                Icon(
                                    imageVector = AppIcons.Status.error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            is ValidationResult.Warning -> {
                                Icon(
                                    imageVector = AppIcons.Status.warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                    
                    // Custom trailing icon
                    trailingIcon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null
                        )
                    }
                }
            },
            isError = validationResult is ValidationResult.Error || errorText != null,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            readOnly = readOnly,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    hasFocus = focusState.hasFocus
                    if (focusState.hasFocus) {
                        hasBeenFocused = true
                    }
                    if (!focusState.hasFocus && validateOnFocusLost && validator != null) {
                        validationResult = validator(value)
                    }
                }
        )
        
        // Helper/Error text with character count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // FIX: Smart cast issue - extract values before using
            val currentText = when {
                errorText != null -> errorText
                validationResult is ValidationResult.Error -> {
                    val error = validationResult as ValidationResult.Error
                    error.message
                }
                validationResult is ValidationResult.Warning -> {
                    val warning = validationResult as ValidationResult.Warning
                    warning.message
                }
                helperText != null -> helperText
                else -> ""
            }
            
            AnimatedContent(
                targetState = currentText,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "helper_text"
            ) { text ->
                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            errorText != null || validationResult is ValidationResult.Error -> 
                                MaterialTheme.colorScheme.error
                            validationResult is ValidationResult.Warning -> 
                                MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            
            if (showCharacterCount && maxLength != null) {
                Text(
                    text = "${value.length}/$maxLength",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (value.length >= maxLength) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Email field with built-in validation
 */
@Composable
fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Email",
    placeholder: String = "example@domain.com",
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    required: Boolean = false
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        leadingIcon = AppIcons.Communication.email,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        validator = { email ->
            when {
                email.isEmpty() && required -> ValidationResult.Error("Email is required")
                email.isNotEmpty() && !email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) ->
                    ValidationResult.Error("Invalid email format")
                else -> ValidationResult.Success
            }
        },
        enabled = enabled,
        required = required,
        modifier = modifier
    )
}

/**
 * Password field with visibility toggle
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password",
    placeholder: String = "Enter password",
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    required: Boolean = false,
    validateStrength: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        leadingIcon = AppIcons.Security.lock,
        trailingIcon = if (passwordVisible) {
            AppIcons.Security.visibilityOff
        } else {
            AppIcons.Security.visibility
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        validator = if (validateStrength) { password ->
            when {
                password.isEmpty() && required -> ValidationResult.Error("Password is required")
                password.length < 8 -> ValidationResult.Error("Password must be at least 8 characters")
                !password.any { it.isDigit() } -> ValidationResult.Warning("Add numbers for stronger password")
                !password.any { it.isUpperCase() } -> ValidationResult.Warning("Add uppercase for stronger password")
                else -> ValidationResult.Success
            }
        } else null,
        enabled = enabled,
        required = required,
        modifier = modifier
    )
}

/**
 * Number field with validation
 */
@Composable
fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    leadingIcon: ImageVector? = null,
    min: Double? = null,
    max: Double? = null,
    decimals: Int = 0,
    enabled: Boolean = true,
    required: Boolean = false
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimals > 0) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        formatter = { input ->
            // Allow only numbers and decimal point
            val filtered = if (decimals > 0) {
                input.filter { it.isDigit() || it == '.' }
            } else {
                input.filter { it.isDigit() }
            }
            
            // Limit decimal places
            if (decimals > 0 && filtered.contains('.')) {
                val parts = filtered.split('.')
                if (parts.size == 2 && parts[1].length > decimals) {
                    "${parts[0]}.${parts[1].take(decimals)}"
                } else {
                    filtered
                }
            } else {
                filtered
            }
        },
        validator = { text ->
            val number = text.toDoubleOrNull()
            when {
                text.isEmpty() && required -> ValidationResult.Error("This field is required")
                number == null && text.isNotEmpty() -> ValidationResult.Error("Invalid number")
                min != null && number != null && number < min -> 
                    ValidationResult.Error("Minimum value is $min")
                max != null && number != null && number > max -> 
                    ValidationResult.Error("Maximum value is $max")
                else -> ValidationResult.Success
            }
        },
        enabled = enabled,
        required = required,
        modifier = modifier
    )
}
