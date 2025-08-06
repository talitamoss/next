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
        val moreVert = Icons.Filled.MoreVert
        val moreHoriz = Icons.Filled.MoreHoriz
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
        val cancel = Icons.Filled.Cancel
        val clear = Icons.Filled.Clear
        val favorite = Icons.Filled.Favorite
        val favoriteBorder = Icons.Filled.FavoriteBorder
        val copy = Icons.Filled.ContentCopy
        val paste = Icons.Filled.ContentPaste
        val cut = Icons.Filled.ContentCut
        val undo = Icons.Filled.Undo
        val redo = Icons.Filled.Redo
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
        val lineChart = Icons.Filled.ShowChart
        val dataUsage = Icons.Filled.DataUsage
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
        val weight = Icons.Filled.MonitorWeight
        val note = Icons.Filled.Note
        val habit = Icons.Filled.CheckCircle
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
        val warning = Icons.Filled.Warning
        val admin = Icons.Filled.AdminPanelSettings
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
        val done = Icons.Filled.Done
        val doneAll = Icons.Filled.DoneAll
    }
    
    // Storage Icons
    object Storage {
        val folder = Icons.Filled.Folder
        val folderOpen = Icons.Filled.FolderOpen
        val storage = Icons.Filled.Storage
        val database = Icons.Filled.Storage
        val cloud = Icons.Filled.Cloud
        val cloudUpload = Icons.Filled.CloudUpload
        val cloudDownload = Icons.Filled.CloudDownload
        val cloudSync = Icons.Filled.CloudSync
        val cloudOff = Icons.Filled.CloudOff
        val file = Icons.Filled.InsertDriveFile
        val attachment = Icons.Filled.AttachFile
        val download = Icons.Filled.Download
        val upload = Icons.Filled.Upload
        val save = Icons.Filled.Save
        val saveAlt = Icons.Filled.SaveAlt
    }
    
    // Communication Icons
    object Communication {
        // Both singular and plural for compatibility
        val notification = Icons.Filled.Notifications
        val notifications = Icons.Filled.Notifications
        val notificationActive = Icons.Filled.NotificationsActive
        val notificationOff = Icons.Filled.NotificationsOff
        val email = Icons.Filled.Email
        val message = Icons.Filled.Message
        val chat = Icons.Filled.Chat
        val send = Icons.Filled.Send
        val inbox = Icons.Filled.Inbox
        val drafts = Icons.Filled.Drafts
        val mail = Icons.Filled.Mail
        val markEmailRead = Icons.Filled.MarkEmailRead
        val markEmailUnread = Icons.Filled.MarkEmailUnread
        val cloud = Icons.Filled.Cloud
    }
    
    // UI Control Icons
    object Control {
        val expand = Icons.Filled.ExpandMore
        val collapse = Icons.Filled.ExpandLess
        val chevronRight = Icons.Filled.ChevronRight
        val chevronLeft = Icons.Filled.ChevronLeft
        val arrowUp = Icons.Filled.KeyboardArrowUp
        val arrowDown = Icons.Filled.KeyboardArrowDown
        val arrowLeft = Icons.Filled.KeyboardArrowLeft
        val arrowRight = Icons.Filled.KeyboardArrowRight
        val arrowForward = Icons.Filled.ArrowForward
        val arrowBack = Icons.Filled.ArrowBack
        val firstPage = Icons.Filled.FirstPage
        val lastPage = Icons.Filled.LastPage
        val navigateNext = Icons.Filled.NavigateNext
        val navigateBefore = Icons.Filled.NavigateBefore
    }
    
    // Media Icons
    object Media {
        val play = Icons.Filled.PlayArrow
        val pause = Icons.Filled.Pause
        val stop = Icons.Filled.Stop
        val skipNext = Icons.Filled.SkipNext
        val skipPrevious = Icons.Filled.SkipPrevious
        val fastForward = Icons.Filled.FastForward
        val fastRewind = Icons.Filled.FastRewind
        val volumeUp = Icons.Filled.VolumeUp
        val volumeDown = Icons.Filled.VolumeDown
        val volumeMute = Icons.Filled.VolumeMute
        val volumeOff = Icons.Filled.VolumeOff
        val mic = Icons.Filled.Mic
        val micOff = Icons.Filled.MicOff
        val camera = Icons.Filled.Camera
        val image = Icons.Filled.Image
        val video = Icons.Filled.Videocam
        val photo = Icons.Filled.Photo
        val photoCamera = Icons.Filled.PhotoCamera
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
        val contacts = Icons.Filled.Contacts
        val badge = Icons.Filled.Badge
        val manage = Icons.Filled.ManageAccounts
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
                level <= 6 -> Icons.Filled.Battery6Bar
                else -> Icons.Filled.BatteryFull
            }
        }
        
        /**
         * Get weather icon (placeholder - customize as needed)
         */
        fun getWeatherIcon(condition: String): ImageVector {
            return when (condition.lowercase()) {
                "sunny", "clear" -> Icons.Filled.WbSunny
                "cloudy", "overcast" -> Icons.Filled.Cloud
                "rainy", "rain" -> Icons.Filled.Umbrella
                "stormy", "thunder" -> Icons.Filled.Thunderstorm
                "snowy", "snow" -> Icons.Filled.AcUnit
                "foggy", "fog" -> Icons.Filled.CloudQueue
                else -> Icons.Filled.WbCloudy
            }
        }
        
        /**
         * Get sync status icon
         */
        fun getSyncIcon(status: String): ImageVector {
            return when (status.lowercase()) {
                "synced", "complete" -> Icons.Filled.CloudDone
                "syncing", "pending" -> Icons.Filled.Sync
                "error", "failed" -> Icons.Filled.SyncProblem
                "disabled", "off" -> Icons.Filled.SyncDisabled
                else -> Icons.Filled.CloudSync
            }
        }
    }
}
