package com.rommelguerra.prototipomusichaptic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rommelguerra.prototipomusichaptic.haptics.HapticPreferences
import com.rommelguerra.prototipomusichaptic.models.AppGlobals

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntensitySelector() {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Intensidad de vibración", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("BAJA" to "Baja · 25 %", "MEDIA" to "Media · 60 %", "ALTA" to "Alta · 100 %").forEach { (value, label) ->
                    FilterChip(selected = AppGlobals.intensidadSeleccionada == value,
                        onClick = { HapticPreferences.chooseIntensity(value) }, label = { Text(label) })
                }
            }
            Text("Se aplica a notas, canciones y pruebas del brazalete. Durante una canción cambia en el siguiente pulso. El volumen del audio se ajusta por separado.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
