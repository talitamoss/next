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
        val archive = Icons.Filled.Archive
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
        val palette = Icons.Filled.Palette
        val dashboard = Icons.Filled.Dashboard
        val fullscreen = Icons.Filled.Fullscreen
        val exitFullscreen = Icons.Filled.FullscreenExit
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
        val alcohol = Icons.Filled.LocalBar
        val screenTime = Icons.Filled.PhoneAndroid
        val social = Icons.Filled.People
	val meditation = Icons.Filled.SelfImprovement
	val journal = Icons.Filled.Book
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
        val check = Icons.Filled.Check
        val verified = Icons.Filled.VerifiedUser
        val pause = Icons.Filled.Pause
        val play = Icons.Filled.PlayArrow
        val stop = Icons.Filled.Stop
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
        val share = Icons.Filled.Share
        val link = Icons.Filled.Link
        val linkOff = Icons.Filled.LinkOff
        val phone = Icons.Filled.Phone
        val sms = Icons.Filled.Sms
    }
    
    // Device Icons
    object Device {
        val camera = Icons.Filled.CameraAlt
        val cameraFront = Icons.Filled.CameraFront
        val cameraRear = Icons.Filled.CameraRear
        val mic = Icons.Filled.Mic
        val micOff = Icons.Filled.MicOff
        val microphone = Icons.Filled.Mic
        val sensors = Icons.Filled.Sensors
        val sensorsOff = Icons.Filled.SensorsOff
        val fingerprint = Icons.Filled.Fingerprint
        val bluetooth = Icons.Filled.Bluetooth
        val bluetoothDisabled = Icons.Filled.BluetoothDisabled
        val wifi = Icons.Filled.Wifi
        val wifiOff = Icons.Filled.WifiOff
        val battery = Icons.Filled.Battery0Bar
        val batteryFull = Icons.Filled.BatteryFull
        val batteryCharging = Icons.Filled.BatteryChargingFull
        val brightness = Icons.Filled.Brightness6
        val volume = Icons.Filled.VolumeUp
        val volumeOff = Icons.Filled.VolumeOff
        val vibration = Icons.Filled.Vibration
        val flashOn = Icons.Filled.FlashOn
        val flashOff = Icons.Filled.FlashOff
        val gps = Icons.Filled.GpsFixed
        val gpsOff = Icons.Filled.GpsOff
        val locationOn = Icons.Filled.LocationOn
        val locationOff = Icons.Filled.LocationOff
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
        val firstPage = Icons.Filled.FirstPage
        val lastPage = Icons.Filled.LastPage
        val navigateNext = Icons.Filled.NavigateNext
        val navigateBefore = Icons.Filled.NavigateBefore
        val subdirectoryArrowRight = Icons.Filled.SubdirectoryArrowRight
        val subdirectoryArrowLeft = Icons.Filled.SubdirectoryArrowLeft
    }
    
    // User & Account Icons
    object User {
        val person = Icons.Filled.Person
        val personAdd = Icons.Filled.PersonAdd
        val personRemove = Icons.Filled.PersonRemove
        val people = Icons.Filled.People
        val group = Icons.Filled.Group
        val groupAdd = Icons.Filled.GroupAdd
        val account = Icons.Filled.AccountCircle
        val accountBox = Icons.Filled.AccountBox
        val contacts = Icons.Filled.Contacts
        val badge = Icons.Filled.Badge
        val manage = Icons.Filled.ManageAccounts
        val supervisor = Icons.Filled.SupervisorAccount
    }
    
    // Alert & Feedback Icons
    object Alert {
        val error = Icons.Filled.Error
        val errorOutline = Icons.Filled.ErrorOutline
        val warning = Icons.Filled.Warning
        val info = Icons.Filled.Info
        val help = Icons.Filled.Help
        val helpOutline = Icons.Filled.HelpOutline
        val reportProblem = Icons.Filled.ReportProblem
        val feedback = Icons.Filled.Feedback
        val announcement = Icons.Filled.Announcement
        val notificationImportant = Icons.Filled.NotificationImportant
        val newReleases = Icons.Filled.NewReleases
        val campaign = Icons.Filled.Campaign
    }
}
