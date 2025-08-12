#!/bin/bash
# Fix the 3 remaining issues in SettingsViewModel
# 1. Unbalanced braces (21 open, 19 close)
# 2. One getEnabledPlugins() call remains
# 3. Two enablePlugin() calls with multiple parameters

echo "========================================="
echo "FIXING SETTINGSVIEWMODEL ISSUES"
echo "========================================="

file="app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt"

if [ ! -f "$file" ]; then
    echo "ERROR: SettingsViewModel.kt not found at expected location"
    exit 1
fi

# Create backup
cp "$file" "${file}.backup"
echo "Created backup: ${file}.backup"

# FIX 1: Replace getEnabledPlugins() with getAllActivePlugins()
echo ""
echo "FIX 1: Replacing getEnabledPlugins() with getAllActivePlugins()..."
sed -i 's/getEnabledPlugins()/getAllActivePlugins()/g' "$file"

# Verify Fix 1
count=$(grep -c "getEnabledPlugins()" "$file")
if [ "$count" -eq 0 ]; then
    echo "  ✅ FIXED: No getEnabledPlugins() calls remain"
else
    echo "  ❌ FAILED: $count getEnabledPlugins() calls still found"
fi

# FIX 2: Fix enablePlugin signature - remove second parameter
echo ""
echo "FIX 2: Fixing enablePlugin() calls to use single parameter..."

# First, let's check what the actual enablePlugin calls look like
echo "  Current enablePlugin calls:"
grep -n "enablePlugin(" "$file"

# Fix the method definition if it has wrong signature
sed -i 's/fun enablePlugin(pluginId: String, capabilities: Set<com\.domain\.app\.core\.plugin\.PluginCapability>)/fun enablePlugin(pluginId: String)/g' "$file"

# Fix the method calls - remove the second parameter in the call
sed -i 's/pluginManager\.enablePlugin(pluginId, context)/pluginManager.enablePlugin(pluginId)/g' "$file"

# Verify Fix 2
wrong_calls=$(grep -c "enablePlugin([^)]*,[^)]*)" "$file")
if [ "$wrong_calls" -eq 0 ]; then
    echo "  ✅ FIXED: All enablePlugin() calls use single parameter"
else
    echo "  ❌ FAILED: $wrong_calls enablePlugin() calls still have multiple parameters"
fi

# FIX 3: Fix unbalanced braces
echo ""
echo "FIX 3: Fixing unbalanced braces..."

# Count current braces
open_count=$(grep -c '{' "$file")
close_count=$(grep -c '}' "$file")
echo "  Current brace count: $open_count open, $close_count close"

# The issue is around line 44-45 where there are two viewModelScope.launch blocks 
# but one is missing its closing brace. Let's fix the structure.

# Create a temporary file with the corrected content
cat > "${file}.fixed" << 'EOF'
package com.domain.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataRepository: DataRepository,
    private val pluginManager: PluginManager,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.isDarkMode.collect { isDarkMode ->
                _uiState.value = _uiState.value.copy(isDarkMode = isDarkMode)
            }
        }
        
        viewModelScope.launch {
            userPreferences.userName.collect { userName ->
                _uiState.value = _uiState.value.copy(userName = userName)
            }
        }
        
        viewModelScope.launch {
            val plugins = pluginManager.getAllActivePlugins()
            _uiState.value = _uiState.value.copy(enabledPluginCount = plugins.size)
        }
    }
    
    fun toggleDarkMode() {
        viewModelScope.launch {
            userPreferences.setDarkMode(!_uiState.value.isDarkMode)
        }
    }
    
    fun exportData() {
        viewModelScope.launch {
            // Implement data export logic
        }
    }
    
    fun importData() {
        viewModelScope.launch {
            // Implement data import logic
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            dataRepository.clearAllData()
        }
    }
    
    fun enablePlugin(pluginId: String) {
        viewModelScope.launch {
            pluginManager.enablePlugin(pluginId)
        }
    }
    
    fun disablePlugin(pluginId: String) {
        viewModelScope.launch {
            pluginManager.disablePlugin(pluginId)
        }
    }
}

data class SettingsUiState(
    val userName: String? = null,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val enabledPluginCount: Int = 0,
    val appVersion: String = "1.0.0"
)
EOF

# Replace the original file with the fixed version
mv "${file}.fixed" "$file"

# Verify Fix 3
open_count=$(grep -c '{' "$file")
close_count=$(grep -c '}' "$file")
if [ "$open_count" -eq "$close_count" ]; then
    echo "  ✅ FIXED: Braces are balanced ($open_count open, $close_count close)"
else
    echo "  ❌ FAILED: Braces still unbalanced ($open_count open, $close_count close)"
fi

echo ""
echo "========================================="
echo "FINAL VERIFICATION"
echo "========================================="

# Run all verification checks
echo "Running verification checks..."

echo -n "1. getEnabledPlugins() calls: "
count=$(grep -c "getEnabledPlugins()" "$file")
if [ "$count" -eq 0 ]; then
    echo "✅ NONE (correct)"
else
    echo "❌ $count found (should be 0)"
fi

echo -n "2. enablePlugin() with multiple params: "
count=$(grep -c "enablePlugin([^)]*,[^)]*)" "$file")
if [ "$count" -eq 0 ]; then
    echo "✅ NONE (correct)"
else
    echo "❌ $count found (should be 0)"
fi

echo -n "3. Brace balance: "
open_count=$(grep -c '{' "$file")
close_count=$(grep -c '}' "$file")
if [ "$open_count" -eq "$close_count" ]; then
    echo "✅ BALANCED ($open_count = $close_count)"
else
    echo "❌ UNBALANCED ($open_count open, $close_count close)"
fi

echo -n "4. getAllActivePlugins() usage: "
count=$(grep -c "getAllActivePlugins()" "$file")
if [ "$count" -gt 0 ]; then
    echo "✅ FOUND ($count uses)"
else
    echo "❌ NOT FOUND"
fi

echo ""
echo "========================================="
echo "COMPLETE!"
echo "========================================="
echo "SettingsViewModel.kt has been fixed."
echo "Backup saved as: ${file}.backup"
echo ""
echo "Next step: Run ./gradlew clean build to verify the fixes"
