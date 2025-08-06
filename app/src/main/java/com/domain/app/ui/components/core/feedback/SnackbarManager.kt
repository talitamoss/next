// app/src/main/java/com/domain/app/ui/components/core/feedback/SnackbarManager.kt
package com.domain.app.ui.components.core.feedback

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Global snackbar manager for consistent notifications across the app.
 * Provides a centralized way to show snackbars from anywhere in the app.
 */
object SnackbarManager {
    private val _snackbarEvents = Channel<SnackbarEvent>(Channel.BUFFERED)
    val snackbarEvents = _snackbarEvents.receiveAsFlow()
    
    suspend fun showMessage(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): Boolean {
        return _snackbarEvents.trySend(
            SnackbarEvent(
                message = message,
                actionLabel = actionLabel,
                duration = duration
            )
        ).isSuccess
    }
    
    suspend fun showSuccess(message: String) {
        showMessage(
            message = "✓ $message",
            duration = SnackbarDuration.Short
        )
    }
    
    suspend fun showError(
        message: String,
        actionLabel: String = "Retry",
        onRetry: (() -> Unit)? = null
    ) {
        val event = SnackbarEvent(
            message = "⚠ $message",
            actionLabel = if (onRetry != null) actionLabel else null,
            duration = SnackbarDuration.Long,
            onAction = onRetry
        )
        _snackbarEvents.trySend(event)
    }
    
    suspend fun showInfo(message: String) {
        showMessage(
            message = "ℹ $message",
            duration = SnackbarDuration.Short
        )
    }
    
    suspend fun showUndo(
        message: String,
        onUndo: () -> Unit
    ) {
        val event = SnackbarEvent(
            message = message,
            actionLabel = "Undo",
            duration = SnackbarDuration.Long,
            onAction = onUndo
        )
        _snackbarEvents.trySend(event)
    }
}

/**
 * Data class representing a snackbar event
 */
data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onAction: (() -> Unit)? = null
)

/**
 * Composable that collects and displays snackbar events.
 * This should be called once at the root of your app hierarchy.
 * 
 * Usage in MainActivity or root composable:
 * ```kotlin
 * val snackbarHostState = remember { SnackbarHostState() }
 * SnackbarEventHandler(snackbarHostState)
 * 
 * Scaffold(
 *     snackbarHost = { SnackbarHost(snackbarHostState) },
 *     ...
 * )
 * ```
 */
@Composable
fun SnackbarEventHandler(
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(snackbarHostState) {
        SnackbarManager.snackbarEvents.collect { event ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = event.duration,
                    withDismissAction = event.duration == SnackbarDuration.Indefinite
                )
                
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        event.onAction?.invoke()
                    }
                    SnackbarResult.Dismissed -> {
                        // Handle dismissal if needed
                    }
                }
            }
        }
    }
}

/**
 * Helper composable that provides a Scaffold with built-in snackbar support.
 * This is a convenience wrapper that handles the SnackbarHost setup.
 */
@Composable
fun ScaffoldWithSnackbar(
    modifier: Modifier = androidx.compose.ui.Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Set up the event handler
    SnackbarEventHandler(snackbarHostState)
    
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        content = content
    )
}

/**
 * Extension function to show snackbar from Composable context
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val scope = rememberCoroutineScope()
 *     
 *     Button(onClick = {
 *         scope.launch {
 *             SnackbarManager.showSuccess("Operation completed!")
 *         }
 *     }) {
 *         Text("Show Snackbar")
 *     }
 * }
 * ```
 */
@Composable
fun rememberSnackbarLauncher(): (suspend (String) -> Unit) {
    val scope = rememberCoroutineScope()
    return remember {
        { message ->
            scope.launch {
                SnackbarManager.showMessage(message)
            }
        }
    }
}

/**
 * Usage Examples:
 * 
 * 1. In a Composable:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val scope = rememberCoroutineScope()
 *     
 *     Button(onClick = {
 *         scope.launch {
 *             SnackbarManager.showSuccess("Item saved!")
 *         }
 *     }) {
 *         Text("Save")
 *     }
 * }
 * ```
 * 
 * 2. In a ViewModel:
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     fun deleteItem(id: String) {
 *         viewModelScope.launch {
 *             try {
 *                 repository.delete(id)
 *                 SnackbarManager.showUndo("Item deleted") {
 *                     restoreItem(id)
 *                 }
 *             } catch (e: Exception) {
 *                 SnackbarManager.showError("Failed to delete item")
 *             }
 *         }
 *     }
 * }
 * ```
 * 
 * 3. In MainActivity:
 * ```kotlin
 * @Composable
 * fun MainScreen() {
 *     ScaffoldWithSnackbar(
 *         bottomBar = { ... }
 *     ) { paddingValues ->
 *         // Your content
 *     }
 * }
 * ```
 */
