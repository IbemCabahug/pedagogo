package com.ibem.pedagogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ibem.pedagogo.alarm.AlarmScheduler
import com.ibem.pedagogo.data.dao.ClassSlotDetail
import com.ibem.pedagogo.data.entity.ClassSlot
import com.ibem.pedagogo.data.entity.Subject
import com.ibem.pedagogo.data.entity.SubjectWithSlots
import com.ibem.pedagogo.ui.navigation.Screen
import com.ibem.pedagogo.ui.screens.AddSubjectScreen
import com.ibem.pedagogo.ui.screens.CorScanScreen
import com.ibem.pedagogo.ui.screens.DashboardScreen
import com.ibem.pedagogo.ui.screens.QrSyncScreen
import com.ibem.pedagogo.ui.screens.ScheduleScreen
import com.ibem.pedagogo.ui.theme.PedagogoTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val app by lazy { application as PedagogoApp }
    private val scheduler by lazy { AlarmScheduler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PedagogoTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                var allSubjects by remember { mutableStateOf<List<SubjectWithSlots>>(emptyList()) }
                var todaySlots by remember { mutableStateOf<List<ClassSlotDetail>>(emptyList()) }

                // Collect local database changes reactively
                LaunchedEffect(Unit) {
                    app.database.scheduleDao().getAllSubjectsWithSlots().collectLatest {
                        allSubjects = it
                    }
                }

                val currentDay = remember {
                    when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        Calendar.SUNDAY -> 7
                        else -> 1
                    }
                }

                LaunchedEffect(currentDay) {
                    app.database.scheduleDao().getSlotsForDay(currentDay).collectLatest {
                        todaySlots = it
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute == Screen.Dashboard.route || currentRoute == Screen.Schedule.route) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Dashboard.route,
                                    onClick = {
                                        if (currentRoute != Screen.Dashboard.route) {
                                            navController.navigate(Screen.Dashboard.route)
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            if (currentRoute == Screen.Dashboard.route) Icons.Filled.Today else Icons.Outlined.Today,
                                            contentDescription = "Today"
                                        )
                                    },
                                    label = { Text("Today") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Schedule.route,
                                    onClick = {
                                        if (currentRoute != Screen.Schedule.route) {
                                            navController.navigate(Screen.Schedule.route)
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            if (currentRoute == Screen.Schedule.route) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                            contentDescription = "Schedule"
                                        )
                                    },
                                    label = { Text("Courses") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                slots = todaySlots,
                                onAddClick = { navController.navigate(Screen.AddSubject.route) },
                                onSyncClick = { navController.navigate(Screen.QrSync.route) },
                                onScanClick = { navController.navigate(Screen.CorScan.route) }
                            )
                        }
                        composable(Screen.Schedule.route) {
                            ScheduleScreen(
                                subjects = allSubjects,
                                onAddSubjectClick = { navController.navigate(Screen.AddSubject.route) }
                            )
                        }
                        composable(Screen.QrSync.route) {
                            QrSyncScreen(
                                subjects = allSubjects,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.AddSubject.route) {
                            AddSubjectScreen(
                                onBack = { navController.popBackStack() },
                                onSave = { subject, slots ->
                                    lifecycleScope.launch {
                                        val subjectId = app.database.scheduleDao().insertSubject(subject)
                                        val slotsWithId = slots.map { it.copy(subjectId = subjectId) }
                                        app.database.scheduleDao().insertClassSlots(slotsWithId)

                                        // Schedule exact alarms for new slots
                                        val updatedSlots = app.database.scheduleDao().getAllSlotDetailsList()
                                        for (slot in updatedSlots.filter { it.subjectId == subjectId }) {
                                            scheduler.scheduleAlarmForSlot(slot)
                                        }

                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                        composable(Screen.CorScan.route) {
                            CorScanScreen(
                                onBack = { navController.popBackStack() },
                                onDone = { navController.popBackStack(Screen.Dashboard.route, inclusive = false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
