package com.privacyguardian.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Guardian : Screen("guardian")
    object History : Screen("history")
    object Result : Screen("result")
    object Settings : Screen("settings")
    object Developer : Screen("developer")
    object TextScanner : Screen("textScanner")
    object DocumentScanner : Screen("documentScanner")
    object Protection : Screen("protection")
}
