package com.rommelguerra.prototipomusichaptic.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rommelguerra.prototipomusichaptic.haptics.HapticProtocol
import com.rommelguerra.prototipomusichaptic.models.AppGlobals
import com.rommelguerra.prototipomusichaptic.models.NoteModel
import com.rommelguerra.prototipomusichaptic.ui.components.BottomNavBar
import com.rommelguerra.prototipomusichaptic.ui.components.IntensitySelector
import com.rommelguerra.prototipomusichaptic.ui.navigation.Screen
import com.rommelguerra.prototipomusichaptic.ui.theme.*
import com.rommelguerra.prototipomusichaptic.ui.viewmodels.MainMenuViewModel

@Composable
fun MainMenuScreen(onNavigate: (String) -> Unit, viewModel: MainMenuViewModel = viewModel()) {
    val context = LocalContext.current
    val bluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) viewModel.startScan()
    }
    val permissions = if (Build.VERSION.SDK_INT >= 31)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startBluetooth() {
        try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            if (adapter == null) Toast.makeText(context, "Bluetooth no disponible", Toast.LENGTH_SHORT).show()
            else if (!adapter.isEnabled) bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else viewModel.startScan()
        } catch (_: SecurityException) {
            Toast.makeText(context, "Permite el acceso a dispositivos cercanos", Toast.LENGTH_LONG).show()
        }
    }
    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) startBluetooth()
        else Toast.makeText(context, "Permite Bluetooth para encontrar el brazalete", Toast.LENGTH_LONG).show()
    }
    fun requestScan() {
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) startBluetooth()
        else permissionsLauncher.launch(permissions)
    }
    DisposableEffect(viewModel) { onDispose { viewModel.leaveScreen() } }
    Scaffold(containerColor = BackgroundGray,
        bottomBar = { BottomNavBar(Screen.MainMenu.route, onNavigate) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val columns = if (LocalDensity.current.fontScale >= 1.3f) 2 else if (maxWidth >= 600.dp) 4 else 3
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("MUSICHAPTIC", style = MaterialTheme.typography.labelMedium, color = VerdePrimario)
                            Text("Hola, ${viewModel.userName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, "Configuración", tint = VerdePrimario)
                        }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(DarkCard, VerdePrimario)), RoundedCornerShape(24.dp)).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFFA9E2C8), modifier = Modifier.size(32.dp))
                        Text("Siente cada nota", color = White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Explora la música a través de tus actuadores izquierdo y derecho.", color = Color(0xFFE0F2E9))
                        Text("2 actuadores · 12 notas", color = White, style = MaterialTheme.typography.labelLarge)
                    }
                }
                item { BraceletCard(viewModel, { requestScan() }) }
                item { IntensitySelector() }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Explora las notas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (AppGlobals.dispositivoSalida == "BRAZALETE") {
                            if (viewModel.isConnected) "Do empieza a la izquierda y Si termina a la derecha."
                            else "Conecta el brazalete para sentir las notas."
                        } else "Salida actual: celular. Conecta el brazalete para sentir ambos lados.",
                            color = ColorTextoSecundario, style = MaterialTheme.typography.bodyMedium)
                        if (viewModel.feedbackText.isNotEmpty()) Text(viewModel.feedbackText, color = VerdePrimario)
                    }
                }
                val rows = viewModel.notes.chunked(columns)
                items(rows.size) { index ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows[index].forEach { note ->
                            NoteTile(note, viewModel.selectedNote == note.nombre,
                                AppGlobals.dispositivoSalida != "BRAZALETE" || viewModel.isConnected,
                                { viewModel.playNote(note) }, Modifier.weight(1f))
                        }
                        repeat(columns - rows[index].size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { onNavigate(Screen.Archive.route) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Icon(Icons.Default.LibraryMusic, null); Spacer(Modifier.width(10.dp)); Text("Abrir pistas y canciones")
                        }
                        OutlinedButton(onClick = { viewModel.stopAll() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp)); Text("Detener vibración")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BraceletCard(vm: MainMenuViewModel, requestScan: () -> Unit) {
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, GrisBorde), colors = CardDefaults.cardColors(containerColor = White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(if (vm.isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    null, tint = VerdePrimario, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (vm.isConnected) "Brazalete listo" else "Conecta tu brazalete",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(vm.statusText, color = ColorTextoSecundario, style = MaterialTheme.typography.bodySmall)
                }
                if (vm.isScanning || vm.isConnecting) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            if (vm.isConnected) {
                Text("Prueba cada lado durante medio segundo.", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.testMotor(true, false) }) { Text("Izquierdo") }
                    OutlinedButton(onClick = { vm.testMotor(false, true) }) { Text("Derecho") }
                    FilledTonalButton(onClick = { vm.testMotor(true, true) }) { Text("Ambos") }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDetails = !showDetails }) { Text(if (showDetails) "Ocultar detalles" else "Ver detalles") }
                    TextButton(onClick = { vm.disconnect() }) { Text("Desconectar") }
                }
                if (showDetails) Text(vm.lastAck, style = MaterialTheme.typography.bodySmall, color = ColorTextoSecundario)
            } else {
                Button(onClick = { if (vm.isScanning) vm.stopScan() else requestScan() }, enabled = !vm.isConnecting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (vm.isScanning) "Detener búsqueda" else "Buscar brazalete")
                }
                if (vm.isConnecting) TextButton(onClick = { vm.disconnect() }) { Text("Cancelar conexión") }
                vm.scannedDevices.forEach { device ->
                    DeviceItem(device, !vm.isConnecting) { vm.connectToDevice(device, context) }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(device: BluetoothDevice, enabled: Boolean, connect: () -> Unit) {
    val name = try { device.name?.takeIf { it.isNotBlank() } ?: "Brazalete Bluetooth" }
        catch (_: SecurityException) { "Brazalete Bluetooth" }
    val address = try { device.address } catch (_: SecurityException) { "" }
    OutlinedCard(onClick = connect, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text("$address · Toca para conectar", style = MaterialTheme.typography.bodySmall, color = ColorTextoSecundario)
        }
    }
}

@Composable
private fun NoteTile(note: NoteModel, selected: Boolean, enabled: Boolean, play: () -> Unit, modifier: Modifier) {
    val pc = HapticProtocol.pitchClass(note.nombre)
    val palette = noteTilePalettes.getOrElse(pc) { NoteTilePalette(VerdePrimario, BackgroundGray) }
    val active = selected && enabled
    val ink = if (!enabled) ColorTextoSecundario else if (active) White else palette.accent
    val top = if (!enabled) BackgroundGray else if (active) palette.accent else palette.surface
    val bottom = if (active) lerp(palette.accent, Color.Black, 0.16f) else White
    Card(onClick = play, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (active) 2.dp else 1.dp,
            if (!enabled) GrisBorde else palette.accent.copy(alpha = if (active) 1f else 0.22f)),
        colors = CardDefaults.cardColors(containerColor = palette.surface)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 136.dp)
            .background(Brush.verticalGradient(listOf(top, bottom))).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(ink.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(if (active) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                        contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                }
                Icon(Icons.Default.PlayArrow, contentDescription = null,
                    tint = ink.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
            }
            Text(if (pc >= 0) HapticProtocol.NAMES[pc] else note.nombre,
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ink)
            Box(Modifier.width(24.dp).height(3.dp).background(ink.copy(alpha = 0.35f), RoundedCornerShape(2.dp)))
        }
    }
}

// Paleta de las doce notas.
private data class NoteTilePalette(val accent: Color, val surface: Color)

private val noteTilePalettes = listOf(
    NoteTilePalette(Color(0xFFAC3B4D), Color(0xFFFCE9ED)), // Do: rose
    NoteTilePalette(Color(0xFFAB4930), Color(0xFFFDEEE7)), // Do sharp: coral
    NoteTilePalette(Color(0xFF966015), Color(0xFFFFF3DA)), // Re: amber
    NoteTilePalette(Color(0xFF77701B), Color(0xFFF7F4D7)), // Re sharp: olive
    NoteTilePalette(Color(0xFF39743A), Color(0xFFEAF3DD)), // Mi: leaf
    NoteTilePalette(Color(0xFF227453), Color(0xFFE0F3E8)), // Fa: emerald
    NoteTilePalette(Color(0xFF1C7177), Color(0xFFE0F3F2)), // Fa sharp: teal
    NoteTilePalette(Color(0xFF286D99), Color(0xFFE4F1FA)), // Sol: blue
    NoteTilePalette(Color(0xFF485CA6), Color(0xFFEAEEFC)), // Sol sharp: indigo
    NoteTilePalette(Color(0xFF714CA0), Color(0xFFF1EAFB)), // La: violet
    NoteTilePalette(Color(0xFF924180), Color(0xFFF8E8F4)), // La sharp: orchid
    NoteTilePalette(Color(0xFFA73F66), Color(0xFFFCE8F0))  // Si: raspberry
)
