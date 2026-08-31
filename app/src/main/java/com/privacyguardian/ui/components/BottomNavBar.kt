package com.privacyguardian.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.privacyguardian.ui.navigation.Screen
import com.privacyguardian.ui.theme.*

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Card,
        contentColor = TextPrimary
    ) {
        val items = listOf(
            Triple(Screen.Home.route, "Home", Icons.Default.Home),
            Triple(Screen.Scan.route, "Scan", Icons.Default.Scanner),
            Triple(Screen.Guardian.route, "Guardian", Icons.Default.Shield),
            Triple(Screen.History.route, "History", Icons.Default.History),
        )
        items.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Safe,
                    selectedTextColor = Safe,
                    indicatorColor = Safe.copy(alpha = 0.15f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
