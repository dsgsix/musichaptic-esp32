package com.rommelguerra.prototipomusichaptic.services

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Metadata de un archivo MIDI subido por el usuario.
 */
data class UserMidiEntry(
    val filename: String,      // nombre real en disco (sanitizado)
    val displayName: String,   // nombre "bonito" para mostrar (sin .mid)
    val sizeBytes: Long,
    val filePath: String
)

/**
 * Persistencia de archivos MIDI subidos por el usuario.
 *
 * Los archivos se copian a `context.filesDir/user_midis/`
 * (almacenamiento privado de la app).
 *
 * Ventajas de este directorio:
 *  - Sobrevive actualizaciones de la app.
 *  - Se limpia automáticamente al desinstalar.
 *  - No requiere permisos de almacenamiento.
 *  - Aislado del resto del sistema.
 */
object UserMidiStorage {

    private const val TAG = "UserMidiStorage"
    private const val DIR_NAME = "user_midis"

    /**
     * Carpeta donde se guardan los MIDIs del usuario.
     * Se crea si no existe.
     */
    fun carpeta(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Lista todos los MIDIs guardados por el usuario,
     * ordenados alfabéticamente.
     */
    fun listar(context: Context): List<UserMidiEntry> {

        val archivos = carpeta(context).listFiles { _, name ->
            name.endsWith(".mid", ignoreCase = true)
        } ?: return emptyList()

        return archivos
            .sortedBy { it.name.lowercase() }
            .map { file ->
                UserMidiEntry(
                    filename    = file.name,
                    displayName = file.nameWithoutExtension,
                    sizeBytes   = file.length(),
                    filePath    = file.absolutePath
                )
            }
    }

    /**
     * Copia un MIDI apuntado por `uri` al almacenamiento privado.
     * Si ya existe un archivo con el mismo nombre, se le agrega
     * un sufijo numérico para evitar colisión.
     *
     * Retorna la entrada guardada, o null si falló.
     */
    suspend fun guardar(
        context: Context,
        uri: Uri,
        contentResolver: ContentResolver
    ): UserMidiEntry? {

        return try {

            val nombreOriginal =
                obtenerNombreArchivo(uri, contentResolver)
                    ?: "midi_${System.currentTimeMillis()}.mid"

            val sanitizado = sanitizar(nombreOriginal)
            val carpeta = carpeta(context)

            // Resolver colisiones agregando sufijo _1, _2...
            var destino = File(carpeta, sanitizado)
            if (destino.exists()) {
                val base = destino.nameWithoutExtension
                var contador = 1
                while (destino.exists()) {
                    destino = File(
                        carpeta,
                        "${base}_$contador.mid"
                    )
                    contador++
                }
            }

            // Copy through a bounded temporary file; never leave a partial MIDI in the catalog.
            val temporary = File.createTempFile("midi_",".tmp",carpeta)
            try {
                contentResolver.openInputStream(uri)?.use { entrada ->
                    temporary.outputStream().use { salida ->
                        val buffer = ByteArray(8192); var total = 0
                        while(true) {
                            currentCoroutineContext().ensureActive()
                            val n = entrada.read(buffer); if(n < 0) break
                            total += n; require(total <= 8*1024*1024) { "MIDI mayor a 8 MB" }
                            salida.write(buffer,0,n)
                        }
                    }
                } ?: return null
                require(temporary.renameTo(destino)) { "No se pudo guardar el MIDI" }
            } finally { temporary.delete() }

            UserMidiEntry(
                filename    = destino.name,
                displayName = destino.nameWithoutExtension,
                sizeBytes   = destino.length(),
                filePath    = destino.absolutePath
            )

        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            Log.e(TAG, "Error guardando MIDI del usuario", e)
            null
        }
    }

    /**
     * Elimina un MIDI del almacenamiento del usuario.
     * Retorna true si se borró.
     */
    fun eliminar(context: Context, filename: String): Boolean {
        if (File(filename).name != filename) return false
        val file = File(carpeta(context), filename)
        return if (file.exists()) file.delete() else false
    }

    /**
     * Devuelve el archivo en disco correspondiente a un filename,
     * o null si no existe.
     */
    fun obtenerArchivo(
        context: Context,
        filename: String
    ): File? {
        if (File(filename).name != filename) return null
        val file = File(carpeta(context), filename)
        return if (file.isFile) file else null
    }

    // ----------------------------------------------------
    // Helpers privados
    // ----------------------------------------------------

    /**
     * Obtiene el nombre "bonito" que reportan las apps del sistema
     * (Files, Drive, etc.) para el URI dado.
     */
    private fun obtenerNombreArchivo(
        uri: Uri,
        contentResolver: ContentResolver
    ): String? {

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )
                if (idx >= 0) {
                    return cursor.getString(idx)
                }
            }
        }
        return uri.lastPathSegment
    }

    /**
     * Sanea un nombre para que sea seguro en el filesystem
     * y termine en .mid.
     */
    private fun sanitizar(nombre: String): String {
        val seguro = nombre.replace(
            Regex("[^A-Za-z0-9._\\- ]"),
            "_"
        ).trim()
        return if (
            seguro.endsWith(".mid", ignoreCase = true)
        ) {
            seguro
        } else {
            "$seguro.mid"
        }
    }
}
