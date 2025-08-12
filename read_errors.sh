echo "========================================="
echo "PHASE 1: VERIFYING ALL FIXES"
echo "========================================="

# 1. Check for plugin.manifest references (should be 0)
echo -n "1. plugin.manifest references: "
count=$(grep -r "plugin\.manifest" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
if [ "$count" -eq 0 ]; then
    echo "✅ FIXED (0 found)"
else
    echo "❌ FAILED ($count still found)"
    grep -n "plugin\.manifest" app/src/main/java --include="*.kt"
fi

# 2. Check for getRiskLevel and getDescription imports
echo -n "2. Extension function imports: "
missing=0
for file in PluginPermissionDialog.kt PluginSecurityScreen.kt; do
    full_path="app/src/main/java/com/domain/app/core/plugin/security/$file"
    if [ -f "$full_path" ]; then
        if ! grep -q "import com.domain.app.core.plugin.getRiskLevel" "$full_path"; then
            echo "  Missing getRiskLevel in $file"
            missing=$((missing + 1))
        fi
        if ! grep -q "import com.domain.app.core.plugin.getDescription" "$full_path"; then
            echo "  Missing getDescription in $file"
            missing=$((missing + 1))
        fi
    fi
done
if [ "$missing" -eq 0 ]; then
    echo "✅ FIXED"
else
    echo "❌ FAILED ($missing imports missing)"
fi

# 3. Check AppIcons import path
echo -n "3. AppIcons import path: "
wrong_count=$(grep -r "import com\.domain\.app\.ui\.components\.AppIcons" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
correct_count=$(grep -r "import com\.domain\.app\.ui\.theme\.AppIcons" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
if [ "$wrong_count" -eq 0 ] && [ "$correct_count" -gt 0 ]; then
    echo "✅ FIXED (using ui.theme.AppIcons)"
else
    echo "❌ FAILED (wrong: $wrong_count, correct: $correct_count)"
fi

# 4. Check ValidationResult import
echo -n "4. ValidationResult import: "
file="app/src/main/java/com/domain/app/ui/dashboard/MultiStageQuickAddDialog.kt"
if [ -f "$file" ]; then
    import_count=$(grep -c "import com.domain.app.core.validation.ValidationResult" "$file")
    if [ "$import_count" -eq 1 ]; then
        echo "✅ FIXED (exactly 1 import)"
    else
        echo "❌ FAILED ($import_count imports found, should be 1)"
    fi
fi

# 5. Check for getAllPlugins usage
echo -n "5. getAllPlugins on correct class: "
wrong_usage=$(grep -r "pluginManager\.getAllPlugins()" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
correct_usage=$(grep -r "pluginRegistry\.getAllPlugins()" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
if [ "$wrong_usage" -eq 0 ]; then
    echo "✅ FIXED (no incorrect usage)"
else
    echo "❌ FAILED ($wrong_usage incorrect calls remain)"
fi

# 6. Check for getEnabledPlugins usage
echo -n "6. getEnabledPlugins replaced: "
old_method=$(grep -r "getEnabledPlugins()" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
new_method=$(grep -r "getAllActivePlugins()" app/src/main/java --include="*.kt" 2>/dev/null | wc -l)
if [ "$old_method" -eq 0 ] && [ "$new_method" -gt 0 ]; then
    echo "✅ FIXED (using getAllActivePlugins)"
else
    echo "❌ FAILED ($old_method old calls remain)"
fi

# 7. Check ExportManager type annotation
echo -n "7. ExportManager data type: "
if grep -q "val data: List<DataPoint> =" app/src/main/java/com/domain/app/core/export/ExportManager.kt; then
    echo "✅ FIXED (explicit type added)"
else
    echo "❌ Type annotation may be missing"
fi

# 8. Check SettingsViewModel braces
echo -n "8. SettingsViewModel braces: "
open_braces=$(grep -c '{' app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt)
close_braces=$(grep -c '}' app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt)
if [ "$open_braces" -eq "$close_braces" ]; then
    echo "✅ FIXED (balanced: $open_braces open, $close_braces close)"
else
    echo "❌ FAILED (unbalanced: $open_braces open, $close_braces close)"
fi

# 9. Check for .collect on non-Flow
echo -n "9. No .collect on List: "
if grep -q "getAllActivePlugins()\.collect" app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt; then
    echo "❌ FAILED (still using .collect on List)"
else
    echo "✅ FIXED (no .collect on getAllActivePlugins)"
fi

# 10. Check enablePlugin signature
echo -n "10. enablePlugin calls: "
wrong_calls=$(grep -c "enablePlugin([^,]*," app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt)
if [ "$wrong_calls" -eq 0 ]; then
    echo "✅ FIXED (single parameter only)"
else
    echo "❌ FAILED ($wrong_calls calls with multiple parameters)"
fi

echo ""
echo "========================================="
echo "PHASE 2: METHOD EXISTENCE VERIFICATION"
echo "========================================="

# Check if getAllActivePlugins exists in PluginManager
echo -n "11. getAllActivePlugins method exists: "
if grep -q "fun getAllActivePlugins()" app/src/main/java/com/domain/app/core/plugin/PluginManager.kt; then
    echo "✅ EXISTS"
else
    echo "❌ MISSING - needs to be added to PluginManager"
fi

# Check if PluginRegistry is imported in PluginsViewModel
echo -n "12. PluginRegistry import in PluginsViewModel: "
if grep -q "import com.domain.app.core.plugin.PluginRegistry" app/src/main/java/com/domain/app/core/plugin/PluginsViewModel.kt; then
    echo "✅ EXISTS"
else
    echo "❌ MISSING"
fi

echo ""
echo "========================================="
echo "PHASE 3: FINAL BUILD TEST"
echo "========================================="
echo "Run: ./gradlew clean build"
