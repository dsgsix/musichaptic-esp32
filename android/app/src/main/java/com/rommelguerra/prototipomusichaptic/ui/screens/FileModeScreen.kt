package com.rommelguerra.prototipomusichaptic.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rommelguerra.prototipomusichaptic.models.MidiCatalog
import com.rommelguerra.prototipomusichaptic.models.MidiCatalogItem
import com.rommelguerra.prototipomusichaptic.services.UserMidiEntry
import com.rommelguerra.prototipomusichaptic.ui.navigation.Screen
import com.rommelguerra.prototipomusichaptic.ui.components.BottomNavBar
import com.rommelguerra.prototipomusichaptic.ui.theme.VerdePrimario
import com.rommelguerra.prototipomusichaptic.ui.viewmodels.FileModeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileModeScreen(
    songName: String?,
    onBack: () -> Unit,
    viewModel: FileModeViewModel = viewModel()
) {
    DisposableEffect(viewModel) { onDispose { viewModel.leaveScreen() } }
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val cacheDir = context.cacheDir

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.analyzeAudioFile(it, contentResolver, cacheDir) }
    }

    // Selector específico para MIDI. GetContent con MIME "audio/midi"
    // no siempre lo reconocen los file managers de Android, así que
    // se usa OpenDocument con múltiples MIME types.
    val midiPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.analyzeMidiFile(it, contentResolver) }
    }

    LaunchedEffect(songName) {
        if (
            songName != null &&
            songName != "external" &&
            songName != "midi"
        ) {
            viewModel.loadFromLibrary(songName)
        }
        if (songName == "midi") {
            viewModel.refrescarMidisGuardados()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (songName) {
                            "external" -> "Analizador Háptico"
                            "midi" -> "Importar MIDI"
                            else -> "Reproductor Musical"
                        }, 
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavBar(currentScreen = Screen.Archive.route, onNavigate = {  })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (songName == "midi") {

                Text(
                    text = "Elige una pieza del repositorio o sube tu propio archivo .mid desde el dispositivo.",
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CATÁLOGO CURADO

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    MidiCatalog.items.forEach { item ->
                        MidiCatalogCard(
                            item = item,
                            onClick = {
                                viewModel.loadMidiFromCatalog(item.slug)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MIS MIDIS (subidos por el usuario)

                    if (viewModel.savedMidis.isNotEmpty()) {

                        Text(
                            text = "Mis MIDIs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        viewModel.savedMidis.forEach { entry ->
                            UserMidiCard(
                                entry = entry,
                                onClick = {
                                    viewModel.cargarMidiGuardado(entry)
                                },
                                onDelete = {
                                    viewModel.eliminarMidiGuardado(entry.filename)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // SUBIR DEL DISPOSITIVO

                    Text(
                        text = "¿Tienes tu propio MIDI?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = {
                            midiPickerLauncher.launch(
                                arrayOf(
                                    "audio/midi",
                                    "audio/x-midi",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF294B77)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Seleccionar del dispositivo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else if (songName == "external") {
                Text(
                    text = "Carga un archivo de música para mapear y traducir automáticamente sus notas a vibraciones hápticas.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = Color(0xFF00D166)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = if (viewModel.fileName == "Ninguno") "Ningún archivo seleccionado" else viewModel.fileName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { filePickerLauncher.launch("audio/*") },
                            modifier = Modifier
                                .width(220.dp)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text("Seleccionar Audio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            } else {

                Text(
                    text = "Reproduciendo pieza de la biblioteca musical.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(64.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = viewModel.fileName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = viewModel.statusText,
                fontSize = 18.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )

            if (viewModel.isAnalyzing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    color = VerdePrimario
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Button(
                    onClick = { viewModel.playSequence() },
                    enabled = viewModel.canPlay && !viewModel.isPlaying,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D166)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(40.dp), tint = Color.White)
                }

                Button(
                    onClick = { viewModel.stopSequence() },
                    enabled = viewModel.isPlaying,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(40.dp), tint = Color.White)
                }
            }
        }
    }
}

/**
 * Tarjeta compacta para un MIDI del catálogo curado.
 */
@Composable
private fun MidiCatalogCard(
    item: MidiCatalogItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3F1F8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF6B3FA0)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = item.compositor,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = "~${item.duracionAproxSeg}s",
                fontSize = 12.sp,
                color = Color(0xFF6B3FA0),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Tarjeta para un MIDI guardado por el usuario.
 * Incluye botón de eliminación.
 */
@Composable
private fun UserMidiCard(
    entry: UserMidiEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEDF3EE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF2A703D)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
            ) {
                Text(
                    text = entry.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = "${entry.sizeBytes / 1024} KB",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color(0xFFB22222)
                )
            }
        }
    }
}
