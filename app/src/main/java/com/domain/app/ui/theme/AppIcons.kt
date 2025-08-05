package com.domain.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon system for consistent visual language across the app.
 * All icons should be accessed through this object to maintain consistency.
 */
object AppIcons {
    
    // Navigation Icons
    object Navigation {
        val home = Icons.Filled.Home
        val dashboard = Icons.Filled.GridView
        val data = Icons.Filled.Analytics
        val settings = Icons.Filled.Settings
        val social = Icons.Filled.People
        val back = Icons.Filled.ArrowBack
        val close = Icons.Filled.Close
        val menu = Icons.Filled.Menu
    }
    
    // Action Icons
    object Action {
        val add = Icons.Filled.Add
        val edit = Icons.Filled.Edit
        val delete = Icons.Filled.Delete
        val save = Icons.Filled.Save
        val share = Icons.Filled.Share
        val search = Icons.Filled.Search
        val filter = Icons.Filled.FilterList
        val sort = Icons.Filled.Sort
        val refresh = Icons.Filled.Refresh
        val more = Icons.Filled.MoreVert
        val moreHoriz = Icons.Filled.MoreHoriz
        val check = Icons.Filled.Check
        val checkCircle = Icons.Filled.CheckCircle
    }
    
    // Data & Time Icons
    object Data {
        val calendar = Icons.Filled.CalendarToday
        val clock = Icons.Filled.AccessTime
        val date = Icons.Filled.DateRange
        val chart = Icons.Filled.BarChart
        val timeline = Icons.Filled.Timeline
        val trending = Icons.Filled.TrendingUp
        val download = Icons.Filled.Download
        val upload = Icons.Filled.Upload
        val sync = Icons.Filled.Sync
    }
    
    // Plugin & Feature Icons
    object Plugin {
        val water = Icons.Filled.WaterDrop
        val mood = Icons.Filled.EmojiEmotions
        val sleep = Icons.Filled.Bedtime
        val exercise = Icons.Filled.FitnessCenter
        val location = Icons.Filled.LocationOn
        val counter = Icons.Filled.Numbers
        val health = Icons.Filled.Favorite
        val productivity = Icons.Filled.WorkHistory
        val custom = Icons.Filled.Extension
    }
    
    // Security Icons
    object Security {
        val lock = Icons.Filled.Lock
        val unlock = Icons.Filled.LockOpen
        val fingerprint = Icons.Filled.Fingerprint
        val shield = Icons.Filled.Shield
        val key = Icons.Filled.Key
        val visibility = Icons.Filled.Visibility
        val visibilityOff = Icons.Filled.VisibilityOff
    }
    
    // Status Icons
    object Status {
        val success = Icons.Filled.CheckCircle
        val error = Icons.Filled.Error
        val warning = Icons.Filled.Warning
        val info = Icons.Filled.Info
        val pending = Icons.Filled.HourglassEmpty
        val active = Icons.Filled.RadioButtonChecked
        val inactive = Icons.Filled.RadioButtonUnchecked
    }
    
    // UI Control Icons
    object Control {
        val expand = Icons.Filled.ExpandMore
        val collapse = Icons.Filled.ExpandLess
        val chevronRight = Icons.Filled.ChevronRight
        val chevronLeft = Icons.Filled.ChevronLeft
        val dragHandle = Icons.Filled.DragHandle
        val clear = Icons.Filled.Clear
        val help = Icons.Filled.Help
    }
    
    // File & Storage Icons
    object Storage {
        val file = Icons.Filled.InsertDriveFile
        val folder = Icons.Filled.Folder
        val cloud = Icons.Filled.Cloud
        val cloudUpload = Icons.Filled.CloudUpload
        val cloudDownload = Icons.Filled.CloudDownload
        val cloudSync = Icons.Filled.CloudSync
        val storage = Icons.Filled.Storage
    }
    
    // Communication Icons
    object Communication {
        val notification = Icons.Filled.Notifications
        val notificationActive = Icons.Filled.NotificationsActive
        val notificationOff = Icons.Filled.NotificationsOff
        val message = Icons.Filled.Message
        val email = Icons.Filled.Email
    }
    
    /**
     * Get icon for a plugin by its ID
     */
    fun getPluginIcon(pluginId: String): ImageVector {
        return when (pluginId) {
            "water" -> Plugin.water
            "mood" -> Plugin.mood
            "sleep" -> Plugin.sleep
            "exercise" -> Plugin.exercise
            "location" -> Plugin.location
            "counter" -> Plugin.counter
            "coffee" -> Plugin.custom  // Using custom icon for coffee, could add dedicated coffee icon
            else -> Plugin.custom
        }
    }
    
    /**
     * Get icon for a plugin category
     */
    fun getCategoryIcon(category: String): ImageVector {
        return when (category.lowercase()) {
            "health" -> Plugin.health
            "productivity" -> Plugin.productivity
            "location" -> Plugin.location
            else -> Plugin.custom
        }
    }
}
