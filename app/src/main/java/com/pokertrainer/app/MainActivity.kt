package com.pokertrainer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import trainer.engine.content.Lessons
import trainer.engine.content.Scenario
import trainer.engine.content.Scenarios

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val progress = ProgressStore(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(progress)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(progress: ProgressStore) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "lessons") {
        composable("lessons") { LessonListScreen(navController, progress) }
        composable("lesson/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            LessonDetailScreen(navController, id)
        }
        composable("practice/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            PracticeScreen(navController, id, progress)
        }
    }
}

@Composable
fun LessonListScreen(nav: NavHostController, progress: ProgressStore) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Lessons", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Lessons.all) { lesson ->
                val best = progress.bestScore(lesson.id)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { nav.navigate("lesson/${lesson.id}") },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(lesson.summary, style = MaterialTheme.typography.bodyMedium)
                        if (best >= 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("Best score: $best%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonDetailScreen(nav: NavHostController, lessonId: String) {
    val lesson = remember(lessonId) { Lessons.byId(lessonId) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(lesson.summary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("Key points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        lesson.keyPoints.forEach { point ->
            Text("•  $point", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        if (lesson.terms.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Terms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lesson.terms.forEach { term ->
                Text(
                    "${term.name}: ${term.definition}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { nav.navigate("practice/$lessonId") }, modifier = Modifier.fillMaxWidth()) {
            Text("Practice this lesson")
        }
    }
}

private enum class PracticePhase { QUESTION, FEEDBACK, DONE }

@Composable
fun PracticeScreen(nav: NavHostController, lessonId: String, progress: ProgressStore) {
    val scenarios = remember(lessonId) { Scenarios.forLesson(lessonId) }
    var index by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(PracticePhase.QUESTION) }
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }

    if (scenarios.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("No drills yet for this lesson.")
        }
        return
    }

    when (phase) {
        PracticePhase.DONE -> {
            LaunchedEffect(Unit) { progress.recordScore(lessonId, correctCount, scenarios.size) }
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Session complete", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text("$correctCount / ${scenarios.size} correct", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    index = 0; correctCount = 0; selectedOptionId = null; phase = PracticePhase.QUESTION
                }) { Text("Practice again") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { nav.popBackStack() }) { Text("Back to lesson") }
            }
        }
        else -> {
            val scenario = scenarios[index]
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Drill ${index + 1} of ${scenarios.size}", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(12.dp))
                ScenarioCard(scenario)
                Spacer(Modifier.height(16.dp))

                scenario.options.forEach { option ->
                    val isSelected = selectedOptionId == option.id
                    val isCorrectOption = option.id == scenario.correctOptionId
                    val showResult = phase == PracticePhase.FEEDBACK
                    OutlinedButton(
                        onClick = {
                            if (phase == PracticePhase.QUESTION) {
                                selectedOptionId = option.id
                                phase = PracticePhase.FEEDBACK
                                if (isCorrectOption) correctCount++
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = if (showResult && isCorrectOption) {
                            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else if (showResult && isSelected) {
                            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                    ) {
                        Text(option.label)
                    }
                }

                if (phase == PracticePhase.FEEDBACK) {
                    Spacer(Modifier.height(16.dp))
                    val wasCorrect = selectedOptionId == scenario.correctOptionId
                    Text(
                        if (wasCorrect) "Correct" else "Not quite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(scenario.explanation, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            selectedOptionId = null
                            if (index + 1 < scenarios.size) {
                                index++
                                phase = PracticePhase.QUESTION
                            } else {
                                phase = PracticePhase.DONE
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (index + 1 < scenarios.size) "Next" else "Finish")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(scenario: Scenario) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(scenario.prompt, style = MaterialTheme.typography.bodyLarge)
            if (scenario.heroCards != "—") {
                Spacer(Modifier.height(8.dp))
                Text("Your hand: ${scenario.heroCards}", fontWeight = FontWeight.Bold)
            }
            scenario.board?.let {
                Text("Board: $it", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            val potLine = buildString {
                append("Pot: ${scenario.potBeforeAction}")
                scenario.betFacing?.let { append("  •  Facing bet: $it") }
            }
            Text(potLine, style = MaterialTheme.typography.labelLarge)
        }
    }
}
