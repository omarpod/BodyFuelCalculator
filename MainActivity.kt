package com.bodyfuel.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

enum class Sex { Male, Female }
enum class ActivityLevel(val labelAr: String, val factor: Double) {
    Low("قليل", 1.2),
    Medium("متوسط", 1.55),
    High("عالي", 1.75)
}
enum class Goal(val labelAr: String, val kcalAdjust: Int) {
    Cut("تنشيف", -400),
    Maintain("ثبات", 0),
    Bulk("تضخيم", 300)
}

data class Result(
    val calories: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen() {
    var sex by remember { mutableStateOf(Sex.Male) }
    var age by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf(ActivityLevel.Medium) }
    var goal by remember { mutableStateOf(Goal.Maintain) }

    var result by remember { mutableStateOf<Result?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("حاسبة السعرات والماكروز لكمال الأجسام", style = MaterialTheme.typography.titleLarge)

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                SegmentedRow(
                    label = "الجنس",
                    options = listOf("ذكر" to Sex.Male, "أنثى" to Sex.Female),
                    selected = sex,
                    onSelect = { sex = it }
                )

                NumberField(label = "العمر (سنة)", value = age, onChange = { age = it })
                NumberField(label = "الطول (سم)", value = heightCm, onChange = { heightCm = it })
                NumberField(label = "الوزن (كغ)", value = weightKg, onChange = { weightKg = it })

                DropdownField(
                    label = "مستوى النشاط",
                    current = activity.labelAr,
                    items = ActivityLevel.entries.map { it.labelAr to it },
                    onSelect = { activity = it }
                )

                DropdownField(
                    label = "الهدف",
                    current = goal.labelAr,
                    items = Goal.entries.map { it.labelAr to it },
                    onSelect = { goal = it }
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        val a = age.toIntOrNull()
                        val h = heightCm.toDoubleOrNull()
                        val w = weightKg.toDoubleOrNull()

                        if (a == null || h == null || w == null) {
                            error = "رجاءً أدخل أرقام صحيحة في العمر/الطول/الوزن."
                            result = null
                            return@Button
                        }
                        if (a !in 10..80 || h !in 120.0..230.0 || w !in 30.0..200.0) {
                            error = "تأكد من القيم: عمر 10-80، طول 120-230 سم، وزن 30-200 كغ."
                            result = null
                            return@Button
                        }

                        error = null
                        result = calculateMacros(
                            sex = sex,
                            age = a,
                            heightCm = h,
                            weightKg = w,
                            activity = activity,
                            goal = goal
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("احسب الآن")
                }
            }
        }

        if (result != null) {
            ResultCard(result!!)
            Text(
                "ملاحظة: البروتين = 2غ/كغ، الدهون = 1غ/كغ، والكارب يُحسب من السعرات المتبقية.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun ResultCard(r: Result) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("النتيجة اليومية", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("السعرات")
                Text("${r.calories} kcal")
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("البروتين")
                Text("${r.proteinG} g")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الدهون")
                Text("${r.fatG} g")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الكارب")
                Text("${r.carbsG} g")
            }
        }
    }
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { ch -> ch.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    current: String,
    items: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (text, value) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun <T> SegmentedRow(
    label: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { idx, (text, value) ->
                SegmentedButton(
                    selected = (value == selected),
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(idx, options.size)
                ) { Text(text) }
            }
        }
    }
}

fun calculateMacros(
    sex: Sex,
    age: Int,
    heightCm: Double,
    weightKg: Double,
    activity: ActivityLevel,
    goal: Goal
): Result {
    // Mifflin-St Jeor
    val bmr = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + if (sex == Sex.Male) 5.0 else -161.0
    val tdee = bmr * activity.factor
    val calories = max(1200.0, tdee + goal.kcalAdjust).roundToInt()

    val proteinG = max(0.0, 2.0 * weightKg).roundToInt()
    val fatG = max(0.0, 1.0 * weightKg).roundToInt()

    val proteinKcal = proteinG * 4
    val fatKcal = fatG * 9
    val carbsKcal = max(0, calories - (proteinKcal + fatKcal))
    val carbsG = (carbsKcal / 4.0).roundToInt()

    return Result(
        calories = calories,
        proteinG = proteinG,
        fatG = fatG,
        carbsG = carbsG
    )
}
