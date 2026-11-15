package com.rommelguerra.prototipomusichaptic

import com.leff.midi.MidiFile
import com.rommelguerra.prototipomusichaptic.haptics.*
import com.rommelguerra.prototipomusichaptic.services.MidiImportService
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.*

class HapticIntegrationTest {
    @Test fun levelAndFrequencyRegressions() { LevelFrequencyTests.main(emptyArray()) }
    @Test fun musicalControlRegressions() { MusicalControlTests.main(emptyArray()) }
    @Test fun dualControlRegressions() { DualControlTests.main(emptyArray()) }
    @Test fun protocolAndYinRegression() { CoreTests.main(emptyArray()) }
    @Test fun musicalFrequenciesKeepPitchClassAcrossOctaves() {
        for(midi in 36..95) {
            val hz = 440.0 * 2.0.pow((midi-69)/12.0)
            assertEquals(midi,MusicalNotes.midi(hz))
            val note = HapticMapping.fromMidi(midi,0.0,500)
            assertEquals(MusicalNotes.name(midi),note.nota)
            if(midi % 12 == 0) assertEquals(0.0,note.ubicacionEspacial,0.0)
            if(midi % 12 == 11) assertEquals(10.0,note.ubicacionEspacial,0.0)
        }
        assertEquals("DO",HapticMapping.fromMidi(MusicalNotes.midi(261.63)!!,0.0,300).nota)
        assertEquals("SI",HapticMapping.fromMidi(MusicalNotes.midi(493.88)!!,0.0,300).nota)
        assertNull(MusicalNotes.midi(Double.NaN)); assertNull(MusicalNotes.midi(0.0))
    }
    @Test fun stopIsPreservedButNewNoteIsNotLost() {
        val q = HapticCommandQueue()
        q.offer(80,0,350,0); q.offer(0,0,0,10)
        q.offer(60,0,350,20); q.offer(0,90,350,30)
        assertEquals(0,q.poll(50)!!.ms)
        val last = q.poll(51)!!; assertEquals(0,last.left); assertEquals(90,last.right)
        assertNull(q.poll(52))
        q.offer(80,0,350,100); assertNull(q.poll(351))
        q.offer(0,0,0,100); assertNotNull(q.poll(10000))
        q.offer(0,0,0,100); q.offer(90,0,350,110); q.offer(0,0,0,120)
        assertEquals(0,q.poll(130)!!.ms); assertNull(q.poll(130))
        q.offer(1,2,350,150); q.clear(); assertNull(q.poll(160))
    }
    @Test fun timelinePreservesDurationAndSilence() {
        fun note(n: String,t: Double,d: Int) = JSONObject().put("n",n).put("t",t).put("d",d).put("f",62.15).put("u",0).put("a",255)
        val json = JSONObject().put("version",2).put("secuencia",JSONArray().put(note("DO",.5,800)).put(note("SI",2.1,900)))
        val events = SequenceTimeline.parse(json.toString())
        assertEquals(500,events[0].startMs); assertEquals(800,events[0].note.duracionMs)
        assertEquals(2100,events[1].startMs); assertEquals(900,events[1].note.duracionMs)
        val legacy = JSONObject().put("secuencia",JSONArray().put(note("DO",0.0,600)).put(note("SI",0.0,800)))
        assertEquals(600,SequenceTimeline.parse(legacy.toString())[1].startMs)
    }
    @Test fun streamingResamplingFindsDoAndSiAtActualSourceRates() {
        for(rate in listOf(8000,16000,22050,44100,48000)) for(midi in listOf(60,71)) {
            val results = mutableListOf<Int>()
            val stream = PcmPitchStream(rate) { result,_ -> if(result.midi >= 0) results.add(result.midi) }
            val hz=440*2.0.pow((midi-69)/12.0)
            repeat(rate) { i -> stream.accept(.3*sin(2*PI*hz*i/rate)+.2*sin(4*PI*hz*i/rate)) }
            assertTrue("No pitch at $rate",results.size >= 10)
            assertTrue("Bad pitch at $rate: $results",results.all { it == midi })
            assertEquals(1.0,stream.seconds(),1e-9)
        }
    }
    private fun midi(track: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(); val d=DataOutputStream(output)
        d.writeBytes("MThd"); d.writeInt(6); d.writeShort(0); d.writeShort(1); d.writeShort(480)
        d.writeBytes("MTrk"); d.writeInt(track.size); d.write(track)
        return output.toByteArray()
    }
    @Test fun midiTempoChangesAndVelocityZeroAreRespected() {
        val data = midi(byteArrayOf(0,0x90.toByte(),60,100, 0x83.toByte(),0x60,0x90.toByte(),60,0,
            0,0xff.toByte(),0x51,3,0x0f,0x42,0x40, 0,0x90.toByte(),71,100,
            0x83.toByte(),0x60,0x80.toByte(),71,0, 0,0xff.toByte(),0x2f,0))
        val notes = MidiImportService.importarDesdeStream(data.inputStream())!!
        assertEquals(listOf("DO","SI"),notes.map { it.nota })
        assertEquals(500,notes[0].duracionMs); assertEquals(.5,notes[1].tiempoInicioS,1e-9)
        assertEquals(1000,notes[1].duracionMs)
    }
    @Test fun midiChannelsDoNotCloseEachOthersNotes() {
        val data = midi(byteArrayOf(0,0x90.toByte(),60,100,
            120,0x91.toByte(),60,100,
            120,0x80.toByte(),60,0,
            0x81.toByte(),0x70,0x81.toByte(),60,0,
            0,0xff.toByte(),0x2f,0))
        val notes = MidiImportService.importarDesdeStream(data.inputStream())!!
        assertEquals(2,notes.size)
        assertEquals(.125,notes[1].tiempoInicioS,1e-9)
        assertEquals(375,notes[1].duracionMs)
    }
    @Test fun bundledMidiCatalogStillLoads() {
        val directory=File("src/main/assets/midi")
        assertTrue(directory.isDirectory)
        val files=directory.listFiles()!!.filter { it.extension == "mid" }
        assertEquals(10,files.size)
        for(file in files) {
            val notes=file.inputStream().use { MidiImportService.importarDesdeStream(it) }
            assertTrue("Empty ${file.name}",!notes.isNullOrEmpty())
        }
    }
    @Test fun malformedMidiIsRejectedPromptly() {
        for(data in listOf(byteArrayOf(),midi(byteArrayOf(0,60,100)),midi(byteArrayOf(0x80.toByte(),0x80.toByte(),0x80.toByte(),0x80.toByte(),0)))) {
            try { MidiFile(data.inputStream()); fail("Malformed MIDI accepted") } catch (_: IllegalArgumentException) {} catch (_: IllegalStateException) {}
        }
    }
}
