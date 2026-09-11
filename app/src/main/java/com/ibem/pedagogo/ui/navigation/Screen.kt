package com.ibem.pedagogo.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Today")
    object Schedule : Screen("schedule", "Schedule")
    object AddSubject : Screen("add_subject", "Add Subject")
    object QrSync : Screen("qr_sync", "Sync with Web")
}
