/**
 * feature-outdoor / OutdoorPackingScreen.kt
 *
 * Pantalla R2 (Compose) STATELESS de la vertical Outdoor: combina estado de preparación
 * (Trip Gate), señales de AwarenessOS y la lista de empaque, con REVELACIÓN PROGRESIVA.
 *
 * Reglas de UX: nunca "seguro"/"sin riesgo"; severidad por icono+etiqueta+texto (no solo
 * color); blockers siempre visibles (no detrás de acordeón); señales descartables; detalle
 * técnico colapsable. No está cableada a la nav salvo con el flag OUTDOOR_R1_ENABLED.
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eco.humanos.android.core.outdoor.awareness.AwarenessSignal
import eco.humanos.android.core.outdoor.awareness.SignalSeverity
import eco.humanos.android.core.outdoor.gate.ComplexityClass
import eco.humanos.android.core.outdoor.gate.Finding
import eco.humanos.android.core.outdoor.gate.ReadinessState
import eco.humanos.android.core.outdoor.gate.TripGateResult

@Composable
fun OutdoorPackingScreen(
    state: OutdoorPackingUiState,
    onTogglePacked: (String, Boolean) -> Unit,
    onDismissSignal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text(state.title.ifBlank { "Salida Outdoor" }, style = MaterialTheme.typography.titleLarge) }

        if (state.degraded == DegradedReason.NO_OUTING) {
            item { InfoCard("Sin datos de salida", "No hay una salida cargada para mostrar preparación.") }
            return@LazyColumn
        }

        val gate = state.gate
        if (gate != null) {
            item { ReadinessHeader(gate) }

            // Blockers SIEMPRE visibles y prioritarios (no en acordeón).
            if (gate.blockingFindings.isNotEmpty()) {
                item { SectionLabel("Faltantes críticos") }
                items(gate.blockingFindings, key = { "b-" + it.code }) { FindingRow(it) }
            }
            if (gate.warnings.isNotEmpty()) {
                item { SectionLabel("Advertencias") }
                items(gate.warnings, key = { "w-" + it.code }) { FindingRow(it) }
            }
            if (gate.pendingRequirements.isNotEmpty()) {
                item {
                    Text(
                        "Pendientes: " + gate.pendingRequirements.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // Señales situacionales (descartables).
        if (state.signals.isNotEmpty()) {
            item { SectionLabel("Avisos de AwarenessOS") }
            items(state.signals, key = { "s-" + it.code }) { signal ->
                SignalRow(signal, onDismiss = { onDismissSignal(signal.code) })
            }
        }

        // Detalle técnico colapsable. Revelación progresiva: expandido por defecto solo en
        // salidas no triviales.
        if (gate != null) {
            item { TechnicalDetail(gate, expandedByDefault = gate.complexityClass != ComplexityClass.SIMPLE) }
        }

        // Lista de empaque por categoría.
        item { HorizontalDivider() }
        item {
            Text(
                "Preparación: ${state.progress.packed}/${state.progress.total} empacado · " +
                    "${state.summary.mandatoryPending} obligatorios pendientes",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.groups.forEach { group ->
            item { SectionLabel(group.category.name) }
            items(group.items, key = { "i-" + it.key }) { pkItem ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pkItem.packed, onCheckedChange = { c -> onTogglePacked(pkItem.key, c) })
                    Spacer(Modifier.width(8.dp))
                    Text("${pkItem.name} ×${pkItem.quantity} (${pkItem.unit})")
                }
            }
        }
    }
}

// ── Readiness (icono + etiqueta + texto, nunca "seguro") ──

private data class Badge(val icon: ImageVector, val label: String)

private fun readinessBadge(state: ReadinessState): Badge = when (state) {
    ReadinessState.GREEN -> Badge(Icons.Filled.CheckCircle, "Preparación al día")
    ReadinessState.YELLOW -> Badge(Icons.Outlined.Warning, "Revisar pendientes")
    ReadinessState.RED -> Badge(Icons.Filled.Warning, "Faltantes críticos")
    ReadinessState.GRAY -> Badge(Icons.Outlined.Info, "Información insuficiente")
}

private fun complexityLabel(c: ComplexityClass): String = when (c) {
    ComplexityClass.SIMPLE -> "Simple"
    ComplexityClass.MODERATE -> "Moderada"
    ComplexityClass.ADVANCED -> "Avanzada"
    ComplexityClass.HIGH_MOUNTAIN -> "Alta montaña"
}

@Composable
private fun ReadinessHeader(gate: TripGateResult) {
    val badge = readinessBadge(gate.readinessState)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(badge.icon, contentDescription = badge.label)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(badge.label, style = MaterialTheme.typography.titleMedium)
                Text("Complejidad: ${complexityLabel(gate.complexityClass)}", style = MaterialTheme.typography.bodyMedium)
                Text("Esto indica preparación y faltantes; no certifica seguridad.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun severityBadge(severity: SignalSeverity): Badge = when (severity) {
    SignalSeverity.CRITICO -> Badge(Icons.Filled.Warning, "Crítico")
    SignalSeverity.ADVERTENCIA -> Badge(Icons.Outlined.Warning, "Advertencia")
    SignalSeverity.INFO -> Badge(Icons.Outlined.Info, "Info")
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun FindingRow(finding: Finding) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Icon(Icons.Filled.Warning, contentDescription = "Bloqueante")
        Spacer(Modifier.width(8.dp))
        Column {
            Text(finding.title, style = MaterialTheme.typography.bodyMedium)
            Text(finding.detail, style = MaterialTheme.typography.bodySmall)
            finding.recommendation?.let { Text("→ $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun SignalRow(signal: AwarenessSignal, onDismiss: () -> Unit) {
    val badge = severityBadge(signal.severity)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(badge.icon, contentDescription = badge.label)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${badge.label} · ${signal.title}", style = MaterialTheme.typography.bodyMedium)
            Text(signal.detail, style = MaterialTheme.typography.bodySmall)
            signal.action?.let { Text("→ $it", style = MaterialTheme.typography.bodySmall) }
        }
        TextButton(onClick = onDismiss) { Text("Descartar") }
    }
}

@Composable
private fun TechnicalDetail(gate: TripGateResult, expandedByDefault: Boolean) {
    var expanded by remember { mutableStateOf(expandedByDefault) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics { contentDescription = if (expanded) "Ocultar detalle técnico" else "Mostrar detalle técnico" }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Detalle técnico", style = MaterialTheme.typography.titleSmall)
        }
        if (expanded) {
            Text("Reglas: ${gate.rulesetVersion}", style = MaterialTheme.typography.bodySmall)
            gate.complexityFactors.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            gate.recommendations.forEach { Text("→ $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
