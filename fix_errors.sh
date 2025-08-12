#!/bin/bash
# SYSTEMATIC FIX SCRIPT - Following Project Verification Checklist
# This script applies all fixes systematically across the entire codebase

echo "========================================="
echo "PHASE 1: RECONNAISSANCE - Verify Current State"
echo "========================================="

# Verify current errors exist
echo "Checking for plugin.manifest references (should find 3)..."
grep -n "plugin\.manifest" app/src/main/java/com/domain/app/core/plugin/security/*.kt

echo "Checking for getRiskLevel without import (should find files)..."
grep -l "getRiskLevel()" app/src/main/java/com/domain/app/core/plugin/security/*.kt | while read file; do
    if ! grep -q "import.*getRiskLevel" "$file"; then
        echo "Missing import in: $file"
    fi
done

echo "========================================="
echo "PHASE 2: SYSTEMATIC FIXES"
echo "========================================="

# FIX 1: Replace ALL occurrences of plugin.manifest with plugin.metadata
echo "FIX 1: Replacing plugin.manifest with plugin.metadata..."
find app/src/main/java -name "*.kt" -type f -exec grep -l "plugin\.manifest" {} \; | while read file; do
    echo "  Fixing: $file"
    sed -i 's/plugin\.manifest/plugin.metadata/g' "$file"
done

# Verify Fix 1
echo "  Verifying: Checking for remaining plugin.manifest..."
if grep -r "plugin\.manifest" app/src/main/java --include="*.kt" 2>/dev/null; then
    echo "  WARNING: plugin.manifest still found!"
else
    echo "  ✓ Fix 1 complete: No plugin.manifest references remain"
fi

# FIX 2: Add missing imports for extension functions
echo "FIX 2: Adding missing imports for getRiskLevel and getDescription..."

# Function to add imports after package declaration
add_imports_to_file() {
    local file=$1
    local package_line=$(grep -n "^package " "$file" | cut -d: -f1)
    
    if [ -z "$package_line" ]; then
        echo "  ERROR: No package declaration in $file"
        return 1
    fi
    
    # Check if imports already exist
    if ! grep -q "import com.domain.app.core.plugin.getRiskLevel" "$file"; then
        # Insert after package line (package_line + 1 for blank line, +2 for import)
        sed -i "$((package_line + 2))i\\import com.domain.app.core.plugin.getRiskLevel" "$file"
        echo "  Added getRiskLevel import to $file"
    fi
    
    if ! grep -q "import com.domain.app.core.plugin.getDescription" "$file"; then
        sed -i "$((package_line + 3))i\\import com.domain.app.core.plugin.getDescription" "$file"
        echo "  Added getDescription import to $file"
    fi
}

# Apply to files that need these imports
add_imports_to_file "app/src/main/java/com/domain/app/core/plugin/security/PluginPermissionDialog.kt"
add_imports_to_file "app/src/main/java/com/domain/app/core/plugin/security/PluginSecurityScreen.kt"

# FIX 3: Fix import path for AppIcons
echo "FIX 3: Fixing AppIcons import path..."
find app/src/main/java -name "*.kt" -type f -exec grep -l "import com\.domain\.app\.ui\.components\.AppIcons" {} \; | while read file; do
    echo "  Fixing import in: $file"
    sed -i 's/import com\.domain\.app\.ui\.components\.AppIcons/import com.domain.app.ui.theme.AppIcons/g' "$file"
done

# FIX 4: Add ValidationResult import
echo "FIX 4: Adding ValidationResult import to MultiStageQuickAddDialog.kt..."
file="app/src/main/java/com/domain/app/ui/dashboard/MultiStageQuickAddDialog.kt"
if [ -f "$file" ]; then
    if ! grep -q "import com.domain.app.core.validation.ValidationResult" "$file"; then
        package_line=$(grep -n "^package " "$file" | cut -d: -f1)
        sed -i "$((package_line + 2))i\\import com.domain.app.core.validation.ValidationResult" "$file"
        echo "  Added ValidationResult import"
    fi
fi

# FIX 5: Fix when expression in PluginSecurityScreen.kt
echo "FIX 5: Adding else branch to when expression..."
file="app/src/main/java/com/domain/app/core/plugin/security/PluginSecurityScreen.kt"
if [ -f "$file" ]; then
    # Find the specific when expression at line 150 and add else branch
    # This is tricky with sed, so we'll use a more targeted approach
    sed -i '/text = when (capability\.getRiskLevel())/,/^[[:space:]]*}/ {
        /RiskLevel\.UNKNOWN ->/a\
        else -> ""
    }' "$file"
    echo "  Added else branch to when expression"
fi

# FIX 6: Fix method calls in ViewModels
echo "FIX 6: Fixing ViewModel method calls..."

# Fix PluginsViewModel.kt
file="app/src/main/java/com/domain/app/core/plugin/PluginsViewModel.kt"
if [ -f "$file" ]; then
    # Change pluginManager.getAllPlugins() to pluginRegistry.getAllPlugins()
    sed -i 's/pluginManager\.getAllPlugins()/pluginRegistry.getAllPlugins()/g' "$file"
    
    # Change pluginManager.getEnabledPlugins() to pluginManager.getAllActivePlugins()
    sed -i 's/pluginManager\.getEnabledPlugins()/pluginManager.getAllActivePlugins()/g' "$file"
    
    # Add PluginRegistry import if not present
    if ! grep -q "import com.domain.app.core.plugin.PluginRegistry" "$file"; then
        package_line=$(grep -n "^package " "$file" | cut -d: -f1)
        sed -i "$((package_line + 2))i\\import com.domain.app.core.plugin.PluginRegistry" "$file"
    fi
    
    # Add pluginRegistry to constructor if not present
    if ! grep -q "private val pluginRegistry: PluginRegistry" "$file"; then
        sed -i '/class PluginsViewModel.*@Inject.*constructor/,/^)/ {
            s/private val pluginManager: PluginManager/private val pluginManager: PluginManager,\n    private val pluginRegistry: PluginRegistry/
        }' "$file"
    fi
    
    echo "  Fixed PluginsViewModel.kt"
fi

# Fix SettingsViewModel.kt
file="app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt"
if [ -f "$file" ]; then
    # Change getEnabledPlugins() to getAllActivePlugins()
    sed -i 's/getEnabledPlugins()/getAllActivePlugins()/g' "$file"
    
    # Fix enablePlugin call with extra parameter
    sed -i 's/enablePlugin(pluginId, true)/enablePlugin(pluginId)/g' "$file"
    
    echo "  Fixed SettingsViewModel.kt"
fi

echo "========================================="
echo "PHASE 3: VERIFICATION"
echo "========================================="

# Verify all fixes
echo "Verification checks:"

echo -n "1. plugin.manifest references: "
count=$(grep -r "plugin\.manifest" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
if [ "$count" -eq 0 ]; then
    echo "✓ FIXED (0 found)"
else
    echo "✗ FAILED ($count still found)"
fi

echo -n "2. getRiskLevel imports: "
missing=0
for file in app/src/main/java/com/domain/app/core/plugin/security/{PluginPermissionDialog,PluginSecurityScreen}.kt; do
    if [ -f "$file" ] && ! grep -q "import.*getRiskLevel" "$file"; then
        missing=$((missing + 1))
    fi
done
if [ "$missing" -eq 0 ]; then
    echo "✓ FIXED"
else
    echo "✗ FAILED ($missing files missing import)"
fi

echo -n "3. AppIcons wrong import: "
count=$(grep -r "import com\.domain\.app\.ui\.components\.AppIcons" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
if [ "$count" -eq 0 ]; then
    echo "✓ FIXED (0 found)"
else
    echo "✗ FAILED ($count still found)"
fi

echo -n "4. ValidationResult import: "
file="app/src/main/java/com/domain/app/ui/dashboard/MultiStageQuickAddDialog.kt"
if [ -f "$file" ] && grep -q "import com.domain.app.core.validation.ValidationResult" "$file"; then
    echo "✓ FIXED"
else
    echo "✗ FAILED"
fi

echo -n "5. getAllPlugins usage: "
count=$(grep -r "pluginManager\.getAllPlugins()" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
if [ "$count" -eq 0 ]; then
    echo "✓ FIXED (0 incorrect calls)"
else
    echo "✗ FAILED ($count incorrect calls remain)"
fi

echo -n "6. getEnabledPlugins usage: "
count=$(grep -r "getEnabledPlugins()" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
if [ "$count" -eq 0 ]; then
    echo "✓ FIXED (0 calls to non-existent method)"
else
    echo "✗ FAILED ($count calls remain)"
fi

echo "========================================="
echo "FINAL BUILD TEST"
echo "========================================="
echo "Run: ./gradlew clean build"
