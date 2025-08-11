// File: com.domain.app.social.ui.contacts.ContactsScreen.kt

package com.domain.app.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.domain.app.social.contracts.SocialContact
import com.domain.app.social.contracts.TrustLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    navController: NavController,
    contacts: List<SocialContact> = sampleMockContacts
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Contacts") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            contacts.forEach { contact ->
                ContactRow(contact = contact, onClick = {
                    // TODO: Handle navigation or actions
                })
                Divider()
            }
        }
    }
}

@Composable
fun ContactRow(contact: SocialContact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Column {
            Text(text = contact.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Trust: ${contact.trustLevel.name}, Online: ${if (contact.isOnline) "Yes" else "No"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

val sampleMockContacts = listOf(
    SocialContact("1", "Cashka", isOnline = true, trustLevel = TrustLevel.CLOSE_FRIEND),
    SocialContact("2", "Jordan", isOnline = false, trustLevel = TrustLevel.FRIEND),
    SocialContact("3", "Sam", isOnline = true, trustLevel = TrustLevel.FAMILY),
)
