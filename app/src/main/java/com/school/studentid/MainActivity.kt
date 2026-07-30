package com.school.studentid

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.school.studentid.ui.ClassFoldersScreen
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

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate("folders") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("folders") {
            ClassFoldersScreen(
                onFolderClick = { className ->
                    navController.navigate("list/${Uri.encode(className)}")
                }
            )
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
                    val presetArg = if (className == ClassConstants.OTHER_CLASSES) "" else Uri.encode(className)
                    navController.navigate("edit?presetClass=$presetArg")
                },
                onStudentClick = { student -> navController.navigate("edit/${student.id}") }
            )
        }

        composable(
            "edit?presetClass={presetClass}",
            arguments = listOf(navArgument("presetClass") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val presetEncoded = backStackEntry.arguments?.getString("presetClass") ?: ""
            val presetClassName = if (presetEncoded.isBlank()) null else Uri.decode(presetEncoded)

            AddEditStudentScreen(
                viewModel = viewModel,
                existingStudent = null,
                presetClassName = presetClassName,
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
