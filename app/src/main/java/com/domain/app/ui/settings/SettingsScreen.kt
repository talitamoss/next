// Add this after the permission request dialog section at the end of SettingsScreen composable:

// Clear data confirmation dialog
if (uiState.showClearDataConfirmation) {
    AlertDialog(
        onDismissRequest = { viewModel.cancelClearData() },
        title = { Text("Clear All Data?") },
        text = {
            Text("This will permanently delete all your recorded data. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.confirmClearAllData() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete All")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelClearData() }) {
                Text("Cancel")
            }
        }
    )
}
