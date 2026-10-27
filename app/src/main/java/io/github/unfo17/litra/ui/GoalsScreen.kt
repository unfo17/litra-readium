package io.github.unfo17.litra.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.unfo17.litra.LibraryViewModel
import io.github.unfo17.litra.data.*
import java.time.*

@Composable
fun GoalsScreen(vm: LibraryViewModel, back: () -> Unit) {
    val goals by vm.goals.collectAsState()
    val checkIns by vm.checkIns.collectAsState()
    val now = System.currentTimeMillis()
    val active = goals.firstOrNull { it.effectiveAt <= now } ?: ReadingGoal(id = 0)
    val pending = goals.firstOrNull { it.effectiveAt > now }
    var period by rememberSaveable(pending?.period, active.period) { mutableStateOf(pending?.period ?: active.period) }
    var minutes by rememberSaveable(pending?.minutes, active.minutes) { mutableStateOf((pending?.minutes ?: active.minutes).toString()) }
    Scaffold(topBar = { PageBar("阅读目标", back) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("把阅读留给每一天", style = MaterialTheme.typography.headlineMedium) }
            item { Text("当前：${GoalPeriods.label(active.period)} ${active.minutes} 分钟。只有阅读器在前台时计时。") }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("DAILY", "WEEKLY", "MONTHLY").forEach { key -> FilterChip(selected = period == key, onClick = { period = key }, label = { Text(GoalPeriods.label(key)) }) } } }
            item { OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(4) }, label = { Text("目标分钟数（1–1440）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), isError = minutes.toIntOrNull()?.let { it !in 1..1440 } ?: true) }
            item { Button(onClick = { vm.action { vm.goalsRepository.configure(period, minutes.toInt()); vm.message.value = "目标已保存，下个周期生效" } }, enabled = minutes.toIntOrNull()?.let { it in 1..1440 } == true) { Text("保存目标") } }
            item { Text("修改从当前周期结束后生效，历史打卡保持原目标。跨周期切换的首个周期可能较短。", style = MaterialTheme.typography.bodySmall) }
            if (pending != null) item { Text("待生效：${GoalPeriods.label(pending.period)} ${pending.minutes} 分钟 · ${date(pending.effectiveAt)}") }
            item { HorizontalDivider(); Text("周期打卡记录", Modifier.padding(top = 20.dp), style = MaterialTheme.typography.titleLarge) }
            if (checkIns.isEmpty()) item { EmptyState("还没有阅读记录", "开始阅读后，会自动累计时长并记录周期目标。") }
            items(checkIns, key = { "${it.period}:${it.start}" }) { checkIn ->
                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${date(checkIn.start)} · ${GoalPeriods.label(checkIn.period)}", style = MaterialTheme.typography.titleMedium)
                    Text("${checkIn.seconds / 60} / ${checkIn.target} 分钟 · ${if (checkIn.completedAt != null) "已达标" else if (checkIn.end <= now) "未达标" else "进行中"}")
                } }
            }
        }
    }
}
private fun date(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate().toString()
