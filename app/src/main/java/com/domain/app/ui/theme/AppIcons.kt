// app/src/main/java/com/domain/app/ui/theme/AppIcons.kt
package com.domain.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon system for consistent visual language across the app.
 * All icons should be accessed through this object to maintain consistency.
 * 
 * Usage:
 * Icon(
 *     imageVector = AppIcons.Action.save,
 *     contentDescription = "Save"
 * )
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
        val close = Icons.Filled.Close
        val favorite = Icons.Filled.Favorite
        val favoriteBorder = Icons.Filled.FavoriteBorder
        val copy = Icons.Filled.ContentCopy
        val paste = Icons.Filled.ContentPaste
        val cut = Icons.Filled.ContentCut
    }
    
    // Data & Time Icons
    object Data {
        val calendar = Icons.Filled.CalendarToday
        val clock = Icons.Filled.AccessTime
        val date = Icons.Filled.DateRange
        val chart = Icons.Filled.BarChart
        val timeline = Icons.Filled.Timeline
        val trending = Icons.Filled.TrendingUp
        val trendingDown = Icons.Filled.TrendingDown
        val download = Icons.Filled.Download
        val upload = Icons.Filled.Upload
        val sync = Icons.Filled.Sync
        val analytics = Icons.Filled.Analytics
        val pieChart = Icons.Filled.PieChart
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
        val energy = Icons.Filled.BatteryChargingFull
        val food = Icons.Filled.Restaurant
        val medication = Icons.Filled.Medication
    }
    
    // Security Icons
    object Security {
        val lock = Icons.Filled.Lock
        val unlock = Icons.Filled.LockOpen
        val fingerprint = Icons.Filled.Fingerprint
        val shield = Icons.Filled.Shield
        val security = Icons.Filled.Security
        val key = Icons.Filled.Key
        val visibility = Icons.Filled.Visibility
        val visibilityOff = Icons.Filled.VisibilityOff
        val verified = Icons.Filled.VerifiedUser
        val privacy = Icons.Filled.PrivacyTip
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
        val online = Icons.Filled.Circle
        val offline = Icons.Filled.CircleNotifications
        val sync = Icons.Filled.Sync
        val syncDisabled = Icons.Filled.SyncDisabled
        val syncProblem = Icons.Filled.SyncProblem
    }
    
    // UI Control Icons
    object Control {
        val expand = Icons.Filled.ExpandMore
        val collapse = Icons.Filled.ExpandLess
        val chevronRight = Icons.Filled.ChevronRight
        val chevronLeft = Icons.Filled.ChevronLeft
        val arrowUp = Icons.Filled.KeyboardArrowUp
        val arrowDown = Icons.Filled.KeyboardArrowDown
        val arrowForward = Icons.Filled.ArrowForward
        val arrowBack = Icons.Filled.ArrowBack
        val dragHandle = Icons.Filled.DragHandle
        val clear = Icons.Filled.Clear
        val help = Icons.Filled.Help
        val helpOutline = Icons.Filled.HelpOutline
        val settings = Icons.Filled.Settings
    }
    
    // File & Storage Icons
    object Storage {
        val folder = Icons.Filled.Folder
        val folderOpen = Icons.Filled.FolderOpen
        val archive = Icons.Filled.Archive
        val cloud = Icons.Filled.Cloud
        val cloudUpload = Icons.Filled.CloudUpload
        val cloudDownload = Icons.Filled.CloudDownload
        val cloudOff = Icons.Filled.CloudOff
        val save = Icons.Filled.Save
        val file = Icons.Filled.InsertDriveFile
        val attachment = Icons.Filled.AttachFile
        val download = Icons.Filled.Download
        val upload = Icons.Filled.Upload
        val delete = Icons.Filled.Delete
        val deleteForever = Icons.Filled.DeleteForever
    }
    
    // Communication Icons
    object Communication {
        val email = Icons.Filled.Email
        val phone = Icons.Filled.Phone
        val message = Icons.Filled.Message
        val chat = Icons.Filled.Chat
        val forum = Icons.Filled.Forum
        val send = Icons.Filled.Send
        val notifications = Icons.Filled.Notifications
        val notificationsActive = Icons.Filled.NotificationsActive
        val notificationsOff = Icons.Filled.NotificationsOff
        val comment = Icons.Filled.Comment
        val feedback = Icons.Filled.Feedback
    }
    
    // Media Icons
    object Media {
        val play = Icons.Filled.PlayArrow
        val pause = Icons.Filled.Pause
        val stop = Icons.Filled.Stop
        val skipNext = Icons.Filled.SkipNext
        val skipPrevious = Icons.Filled.SkipPrevious
        val volumeUp = Icons.Filled.VolumeUp
        val volumeDown = Icons.Filled.VolumeDown
        val volumeOff = Icons.Filled.VolumeOff
        val mic = Icons.Filled.Mic
        val micOff = Icons.Filled.MicOff
        val camera = Icons.Filled.Camera
        val image = Icons.Filled.Image
        val video = Icons.Filled.Videocam
    }
    
    // User & Account Icons
    object User {
        val person = Icons.Filled.Person
        val personAdd = Icons.Filled.PersonAdd
        val personRemove = Icons.Filled.PersonRemove
        val group = Icons.Filled.Group
        val groupAdd = Icons.Filled.GroupAdd
        val account = Icons.Filled.AccountCircle
        val profile = Icons.Filled.AccountBox
        val logout = Icons.Filled.Logout
        val login = Icons.Filled.Login
    }
    
    // Utility functions for dynamic icon selection
    object Utils {
        /**
         * Get mood icon based on mood value
         */
        fun getMoodIcon(mood: Int): ImageVector {
            return when (mood) {
                1 -> Icons.Filled.SentimentVeryDissatisfied
                2 -> Icons.Filled.SentimentDissatisfied
                3 -> Icons.Filled.SentimentNeutral
                4 -> Icons.Filled.SentimentSatisfied
                5 -> Icons.Filled.SentimentVerySatisfied
                else -> Icons.Filled.EmojiEmotions
            }
        }
        
        /**
         * Get energy level icon
         */
        fun getEnergyIcon(level: Int): ImageVector {
            return when {
                level <= 2 -> Icons.Filled.BatteryAlert
                level <= 4 -> Icons.Filled.Battery4Bar
                else -> Icons.Filled.BatteryFull
            }
        }
        
        /**
         * Get weather icon (placeholder - customize as needed)
         */
        fun getWeatherIcon(condition: String): ImageVector {
            return when (condition.lowercase()) {
                "sunny" -> Icons.Filled.WbSunny
                "cloudy" -> Icons.Filled.Cloud
                "rainy" -> Icons.Filled.Thunderstorm
                else -> Icons.Filled.WbCloudy
            }
        }
    }
}
