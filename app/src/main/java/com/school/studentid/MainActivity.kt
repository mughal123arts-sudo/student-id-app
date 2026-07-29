package com.school.studentid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.school.studentid.ui.AddEditStudentScreen
import com.school.studentid.ui.AdminLoginScreen
import com.school.studentid.ui.StudentListScreen

class MainActivity : ComponentActivity() {

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate("list") {
                        // remove login from back stack so back button doesn't return to it
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("list") {
            StudentListScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate("edit") },
                onStudentClick = { student -> navController.navigate("edit/${student.id}") }
            )
        }

        composable("edit") {
            AddEditStudentScreen(
                viewModel = viewModel,
                existingStudent = null,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            "edit/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.IntType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getInt("studentId") ?: 0
            var student by remember { mutableStateOf<Student?>(null) }

            LaunchedEffect(studentId) {
                student = viewModel.students.value.find { it.id == studentId }
            }

            AddEditStudentScreen(
                viewModel = viewModel,
                existingStudent = student,
                onDone = { navController.popBackStack() }
            )
        }
    }
}
