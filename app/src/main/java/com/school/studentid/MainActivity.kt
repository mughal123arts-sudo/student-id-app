package com.school.studentid

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.school.studentid.ui.AddEditStudentScreen
import com.school.studentid.ui.AdminLoginScreen
import com.school.studentid.ui.HomeScreen
import com.school.studentid.ui.SettingsScreen
import com.school.studentid.ui.SetupScreen
import com.school.studentid.ui.SplashScreen
import com.school.studentid.ui.StudentDetailScreen
import com.school.studentid.ui.StudentListScreen
import com.school.studentid.ui.theme.StudentIDAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudentIDAppTheme {
                Surface(modifier = Modifier) {
                    AppNavHost(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(viewModel: StudentViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(
                onTimeout = {
                    val next = if (AppPreferences.isSetupComplete(context)) "login" else "setup"
                    navController.navigate(next) { popUpTo("splash") { inclusive = true } }
                }
            )
        }

        composable("setup") {
            SetupScreen(
                onSetupDone = {
                    navController.navigate("login") { popUpTo("setup") { inclusive = true } }
                }
            )
        }

        composable("login") {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onFolderClick = { className -> navController.navigate("list/${Uri.encode(className)}") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            "list/{className}",
            arguments = listOf(navArgument("className") { type = NavType.StringType })
        ) { backStackEntry ->
            val className = Uri.decode(backStackEntry.arguments?.getString("className") ?: "")

            StudentListScreen(
                viewModel = viewModel,
                classFolder = className,
                onBackClick = { navController.popBackStack() },
                onAddClick = {
                    navController.navigate("student?studentId=-1&classFolder=${Uri.encode(className)}")
                },
                onStudentClick = { student ->
                    navController.navigate("student?studentId=${student.id}&classFolder=${Uri.encode(className)}")
                },
                onViewDetailsClick = { student ->
                    navController.navigate("details/${student.id}")
                }
            )
        }

        composable(
            "student?studentId={studentId}&classFolder={classFolder}",
            arguments = listOf(
                navArgument("studentId") { type = NavType.IntType; defaultValue = -1 },
                navArgument("classFolder") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getInt("studentId") ?: -1
            val classFolder = Uri.decode(backStackEntry.arguments?.getString("classFolder") ?: "")

            var existingStudent by remember { mutableStateOf<Student?>(null) }
            var ready by remember { mutableStateOf(studentId == -1) }

            LaunchedEffect(studentId) {
                if (studentId != -1) {
                    existingStudent = viewModel.getStudentById(studentId)
                    ready = true
                }
            }

            if (ready) {
                val isLocked = classFolder.isNotBlank() && classFolder != ClassConstants.OTHER_CLASSES
                AddEditStudentScreen(
                    viewModel = viewModel,
                    existingStudent = existingStudent,
                    presetClassName = if (classFolder.isBlank()) null else classFolder,
                    lockClassField = isLocked,
                    onDone = { navController.popBackStack() }
                )
            }
        }

        composable(
            "details/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.IntType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getInt("studentId") ?: 0
            var student by remember { mutableStateOf<Student?>(null) }

            LaunchedEffect(studentId) {
                student = viewModel.getStudentById(studentId)
            }

            StudentDetailScreen(student = student, onBackClick = { navController.popBackStack() })
        }
    }
}
