/* Vibrafil Dual v5.1. ESP32-WROOM. Dos salidas GPIO27/GPIO16.
 * Recibe niveles BLE v1 y notas por texto/v3; no decodifica MP3/PCM.
 * Solo loop escribe PWM. ACK separado para no bloquear los vencimientos.
 */
#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <esp_system.h>
#include <atomic>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>
#include "HapticConfig.h"
#include "HapticEngine.h"
#include <Preferences.h>
#include "ConsoleLine.h"
#include "ModulePwm.h"
#include "OutputMath.h"
#include "BenchCommands.h"

namespace {
constexpr char SERVICE_UUID[] = "12345678-1234-1234-1234-1234567890ab";
constexpr char COMMAND_UUID[] = "abcdefab-1234-5678-1234-abcdefabcdef";
constexpr char STATUS_UUID[] = "abcdefac-1234-5678-1234-abcdefabcdef";
constexpr char CAPS_UUID[] = "abcdefad-1234-5678-1234-abcdefabcdef";
constexpr char MUSICAL_CAPS_UUID[] = "abcdefae-1234-5678-1234-abcdefabcdef";
constexpr char LEVEL_CAPS_UUID[] = "abcdefaf-1234-5678-1234-abcdefabcdef";
Preferences preferences;
bool preferencesReady = false;
ModulePwm sideOutputs;
BLECharacteristic* statusCharacteristic = nullptr;
BLE2902* statusCccd = nullptr;
std::atomic<bool> connected{false};
std::atomic<bool> advertiseAgain{false};
std::atomic<uint32_t> session{0};
struct Pending {
    haptic::Command command;
    haptic::Result result;
    uint32_t sessionId;
    uint32_t receivedAt;
};
struct Reply {
    uint8_t bytes[haptic::MAX_FRAME_SIZE];
    size_t length;
    uint32_t sessionId;
};
QueueHandle_t pendingQueue = nullptr, replyQueue = nullptr;
haptic::Engine engine;
uint32_t appliedCount = 0, rejectedCount = 0;
uint16_t sideGain = DEFAULT_SIDE_GAIN;
haptic::BenchSweep sweep;
bool traceEnabled = false;
uint32_t lastTraceAt = 0;
haptic::Command lastCommand{};
haptic::Result lastResult = haptic::OK;
haptic::OutputLevels lastSideLevels{};
float lastPwmHz = 0;
bool haveLast = false, lastWasBle = false;

bool writeOutputs() {
    const auto levels = haptic::sideLevels(engine.active.left, engine.active.right, sideGain, engine.active.intensity);
    const bool pwmOk = sideOutputs.set(levels.left, levels.right, engine.active.carrierCentiHz);
    if (!pwmOk) {
        engine.stop(); sideOutputs.off();
    }
    return pwmOk;
}
void stopOutputs() { engine.stop(); writeOutputs(); }
// Si el buffer USB/Serial esta lleno, se omite el log, nunca se espera al terminal.
void logNonBlocking(const char* line) {
    const size_t n = std::strlen(line);
    if (Serial.availableForWrite() >= int(n)) Serial.write(reinterpret_cast<const uint8_t*>(line), n);
}
void printHelp() {
    Serial.println("AYUDA DUAL v5.1 (escribe una orden y pulsa Enter):");
    Serial.println("STOP / STATUS / LAST / HELP / DEFAULTS / SAVE");
    Serial.println("TEST L / TEST R / TEST B");
    Serial.println("PLAY DO / NOTE DO / PLAY SI / PLAY MID / SWEEP");
    Serial.println("GAIN 125 (0..300); afecta la amplitud del reparto, no la frecuencia.");
    Serial.println("FREQDUTY 75 (50..90); aumenta la amplitud FREQ sin cambiar los Hz.");
    Serial.println("SAVE guarda GAIN y FREQDUTY para el siguiente arranque.");
    Serial.println("MODE FREQ (notas, por defecto) / MODE POWER (comparacion de fuerza)");
    Serial.println("TRACE ON / TRACE OFF / NOTE|DO|80|700 / LR|60|60|700");
}
void printLast() {
    if (!haveLast) { logNonBlocking("[LAST] Todavia no hay orden.\n"); return; }
    char line[224];
    snprintf(line, sizeof(line), "[LAST %s] result=%u v=%u L=%u R=%u note=%.2fHz ms=%u intensity=%u%%\n",
        lastWasBle ? "BLE" : "USB", unsigned(lastResult), lastCommand.version,
        lastCommand.left, lastCommand.right,
        lastCommand.carrierCentiHz / 100.0, lastCommand.duration, lastCommand.intensity);
    logNonBlocking(line);
    snprintf(line, sizeof(line), "[APPLIED] L=%u R=%u PWM=%.3fHz MODE=%s dutyL=%lu dutyR=%lu\n",
        lastSideLevels.left, lastSideLevels.right, double(lastPwmHz),
        sideOutputs.frequencyMode() ? "FREQ" : "POWER",
        static_cast<unsigned long>(haptic::moduleDuty(lastSideLevels.left, sideOutputs.frequencyMode() && lastCommand.carrierCentiHz, MODULE_ACTIVE_HIGH, sideOutputs.frequencyDutyPercent())),
        static_cast<unsigned long>(haptic::moduleDuty(lastSideLevels.right, sideOutputs.frequencyMode() && lastCommand.carrierCentiHz, MODULE_ACTIVE_HIGH, sideOutputs.frequencyDutyPercent())));
    logNonBlocking(line);
}
void printStatus() {
    char line[224];
    snprintf(line, sizeof(line), "[PINS v5.1] L=GPIO%d R=GPIO%d outputs=2 center=OFF\n",
        MOTOR_LEFT_PIN, MOTOR_RIGHT_PIN);
    logNonBlocking(line);
    const auto levels = haptic::sideLevels(engine.active.left, engine.active.right, sideGain, engine.active.intensity);
    snprintf(line, sizeof(line), "[STATUS] BLE=%u L=%u R=%u running=%u sweep=%u ok=%lu rejected=%lu\n",
        unsigned(connected.load()), levels.left, levels.right,
        unsigned(engine.running), unsigned(sweep.running()),
        static_cast<unsigned long>(appliedCount), static_cast<unsigned long>(rejectedCount));
    logNonBlocking(line);
    snprintf(line, sizeof(line), "[SETTINGS] MODE=%s GAIN=%u FREQ_DUTY=%u%% PWM=%.3fHz trace=%u nvs=%u\n",
        sideOutputs.frequencyMode() ? "FREQ" : "POWER", sideGain,
        unsigned(sideOutputs.frequencyDutyPercent()), double(sideOutputs.actualHz()),
        unsigned(traceEnabled), unsigned(preferencesReady));
    logNonBlocking(line);
}
// La lectura GATT devuelve el ultimo ACK, no el estado fisico del actuador.
void replyTask(void*) {
    Reply reply{};
    for (;;) {
        if (xQueueReceive(replyQueue, &reply, portMAX_DELAY) != pdTRUE) continue;
        if (!connected.load() || reply.sessionId != session.load()) continue;
        statusCharacteristic->setValue(reply.bytes, reply.length);
        if (connected.load() && reply.sessionId == session.load() && statusCccd->getNotifications())
            statusCharacteristic->notify();
    }
}
void applyCommand(const Pending& p, bool fromBle) {
    if (fromBle && (!connected.load() || p.sessionId != session.load())) return;
    // La conexion puede haber cambiado despues del chequeo inicial de loop().
    // Registrar aqui la sesion evita cortar esta orden valida en la vuelta siguiente.
    if (fromBle) { sweep.stop(); engine.syncSession(p.sessionId); }
    auto result = engine.apply(p.command, p.result, p.receivedAt, millis());
    if (!writeOutputs()) result = haptic::BAD_VALUE;
    // Una desconexion concurrente no debe dejar activa la ultima orden BLE.
    if (fromBle && (!connected.load() || p.sessionId != session.load())) {
        stopOutputs(); return;
    }
    if (result == haptic::OK) ++appliedCount; else ++rejectedCount;
    if (fromBle) {
        Reply reply{}; reply.sessionId = p.sessionId;
        reply.length=haptic::ack(p.command, result, reply.bytes);
        xQueueOverwrite(replyQueue, &reply);
    }
    {
        haveLast = true; lastCommand = p.command; lastResult = result; lastWasBle = fromBle;
        lastSideLevels = haptic::sideLevels(engine.active.left, engine.active.right, sideGain, engine.active.intensity);
        lastPwmHz = sideOutputs.actualHz();
    }
    if (!fromBle || result != haptic::OK || (traceEnabled && uint32_t(millis()-lastTraceAt) >= 100)) {
        lastTraceAt = millis(); printLast();
        if (result == haptic::BAD_FRAME) logNonBlocking("[ERROR] Formato no reconocido. Usa PLAY DO, NOTE DO o HELP.\n");
        else if (result == haptic::BAD_VALUE) logNonBlocking("[ERROR] Valor fuera de rango o salida PWM no disponible.\n");
        else if (result == haptic::STALE_COMMAND) logNonBlocking("[ERROR] Orden vencida; no se reproduce atrasada.\n");
        else if (result == haptic::CENTER_UNAVAILABLE) logNonBlocking("[ERROR] Firmware dual: canal central deshabilitado.\n");
        if (!engine.running) logNonBlocking("[OFF] Salidas apagadas.\n");
    }
}

class ServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer*) override {
        statusCccd->setNotifications(false);
        session.fetch_add(1); connected.store(true); advertiseAgain.store(false);
    }
    void onDisconnect(BLEServer*) override {
        connected.store(false); session.fetch_add(1); advertiseAgain.store(true);
    }
};
class CapabilitiesCallbacks : public BLECharacteristicCallbacks {
    void onRead(BLECharacteristic* characteristic) override {
        uint8_t caps[]={0x43,1,2,0};
        characteristic->setValue(caps,sizeof(caps));
    }
};
class CommandCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* characteristic) override {
        Pending p{};
        p.sessionId = session.load(); p.receivedAt = millis();
        if (!connected.load()) return;
        const std::string bytes = characteristic->getValue();
        if (!bytes.empty() && uint8_t(bytes[0]) == 0x48) {
            p.result = haptic::decode(reinterpret_cast<const uint8_t*>(bytes.data()), bytes.size(), p.command);
        } else {
            p.result = haptic::decodeText(bytes.data(), bytes.size(), p.command, LEGACY_ROUTE_BY_NOTE);
        }
        // Solo el objetivo mas reciente; no acumular notas antiguas cuando llega audio rapido.
        xQueueOverwrite(pendingQueue, &p);
    }
};
// Ajustes USB: SAVE persiste GAIN y FREQDUTY; el modo vuelve a FREQ al reiniciar.
void usbLine(const char* line, size_t length) {
    if (!std::strcmp(line, "STATUS")) { printStatus(); return; }
    if (!std::strcmp(line, "LAST")) { printLast(); return; }
    if (!std::strcmp(line, "HELP")) { stopOutputs(); sweep.stop(); printHelp(); return; }
    if (!std::strcmp(line, "TRACE ON")) { traceEnabled=true; printStatus(); return; }
    if (!std::strcmp(line, "TRACE OFF")) { traceEnabled=false; printStatus(); return; }
    sweep.stop();
    if (!std::strcmp(line, "MODE POWER") || !std::strcmp(line, "MODE FREQ")) {
        stopOutputs(); sideOutputs.setFrequencyMode(!std::strcmp(line, "MODE FREQ"));
        writeOutputs(); printStatus(); return;
    }
    if (!std::strcmp(line, "DEFAULTS")) {
        stopOutputs(); sideGain=DEFAULT_SIDE_GAIN;
        sideOutputs.setFrequencyMode(DEFAULT_FREQUENCY_MODE);
        sideOutputs.setFrequencyDutyPercent(DEFAULT_FREQUENCY_DUTY_PERCENT);
        traceEnabled=false;
        writeOutputs(); printStatus(); return;
    }
    if (!std::strcmp(line, "SAVE")) {
        stopOutputs();
        const bool saved = preferencesReady &&
            preferences.putUShort("gain", sideGain) == sizeof(uint16_t) &&
            preferences.putUChar("fduty", sideOutputs.frequencyDutyPercent()) == sizeof(uint8_t);
        if (saved) logNonBlocking("[SAVE] GAIN y FREQDUTY guardados. MODE siempre inicia en FREQ.\n");
        else logNonBlocking("[ERROR] No se pudieron guardar GAIN/FREQDUTY. Siguen vigentes solo en RAM.\n");
        printStatus(); return;
    }
    if (!std::strncmp(line, "FREQDUTY ", 9)) {
        stopOutputs(); float value;
        if (!haptic::number(line + 9, value) || value != std::floor(value) ||
            value < MIN_FREQUENCY_DUTY_PERCENT || value > MAX_FREQUENCY_DUTY_PERCENT) {
            ++rejectedCount;
            logNonBlocking("[ERROR] Usa FREQDUTY 50..90; salidas OFF.\n");
            return;
        }
        if (!sideOutputs.setFrequencyDutyPercent(uint8_t(value))) {
            ++rejectedCount;
            logNonBlocking("[ERROR] No se pudo ajustar FREQDUTY; salidas OFF.\n");
            return;
        }
        printStatus(); return;
    }
    if (!std::strncmp(line, "GAIN ", 5)) {
        stopOutputs(); float value;
        if (!haptic::number(line + 5, value) || value != std::floor(value) || value > 300) {
            ++rejectedCount; logNonBlocking("[ERROR] Usa GAIN 0..300; salidas OFF.\n"); return;
        }
        sideGain=uint16_t(value); printStatus(); return;
    }
    if (!std::strcmp(line, "SWEEP")) {
        stopOutputs(); sweep.start(millis());
        logNonBlocking("[SWEEP] 12 notas y MID. STOP cancela.\n"); return;
    }
    Pending p{}; p.receivedAt=millis();
    if (!std::strncmp(line, "PLAY ", 5) || !std::strncmp(line, "NOTE ", 5))
        p.result=haptic::benchPlay(line+5, p.command);
    else p.result=haptic::decodeText(line, length, p.command, LEGACY_ROUTE_BY_NOTE);
    applyCommand(p, false);
}
void serialCommands() {
    static haptic::ConsoleLine console;
    // Limite de trabajo para no retrasar expiraciones ante un pegado masivo.
    for (int i = 0; i < 32 && Serial.available(); ++i) {
        const auto event = console.feed(uint8_t(Serial.read()));
        if (event == haptic::ConsoleLine::Event::Ready)
            usbLine(console.text(), std::strlen(console.text()));
        else if (event == haptic::ConsoleLine::Event::Invalid) {
            sweep.stop(); stopOutputs(); ++rejectedCount;
            logNonBlocking("[USB] Linea demasiado larga o con caracteres no admitidos; OFF.\n");
        }
    }
}
void fatal(const char* message) {
    stopOutputs(); Serial.println(message);
    for (;;) delay(1000);
}
} // namespace

void setup() {
    Serial.setTxBufferSize(1024);
    Serial.begin(115200);
    if (!sideOutputs.begin()) fatal("FATAL: no se pudo configurar PWM.");
    stopOutputs();
    preferencesReady = preferences.begin("vibrafil-dual", false);
    if (preferencesReady) {
        const uint16_t saved = preferences.getUShort("gain", DEFAULT_SIDE_GAIN);
        sideGain = saved <= 300 ? saved : DEFAULT_SIDE_GAIN;
        const uint8_t savedDuty = preferences.getUChar("fduty", DEFAULT_FREQUENCY_DUTY_PERCENT);
        if (savedDuty >= MIN_FREQUENCY_DUTY_PERCENT &&
            savedDuty <= MAX_FREQUENCY_DUTY_PERCENT)
            sideOutputs.setFrequencyDutyPercent(savedDuty);
    }
    pendingQueue = xQueueCreate(1, sizeof(Pending));
    replyQueue = xQueueCreate(1, sizeof(Reply));
    if (!pendingQueue || !replyQueue) fatal("FATAL: no se pudo crear el buzon.");

    BLEDevice::init("ESP32 Vibrafil");
    BLEDevice::setMTU(185); // El binario de hasta 16 bytes cabe incluso con MTU 23.
    auto* server = BLEDevice::createServer();
    server->setCallbacks(new ServerCallbacks());
    auto* service = server->createService(BLEUUID(SERVICE_UUID),24);
    auto* command = service->createCharacteristic(COMMAND_UUID,
        BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
    command->setCallbacks(new CommandCallbacks());
    statusCharacteristic = service->createCharacteristic(STATUS_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
    auto* caps=service->createCharacteristic(CAPS_UUID,BLECharacteristic::PROPERTY_READ);
    caps->setCallbacks(new CapabilitiesCallbacks());
    uint8_t capValue[]={0x43,1,2,0};
    caps->setValue(capValue,sizeof(capValue));
    // Extension separada para conservar las capacidades v2 de cuatro bytes.
    auto* musicalCaps=service->createCharacteristic(MUSICAL_CAPS_UUID,BLECharacteristic::PROPERTY_READ);
    const uint16_t exponent = uint16_t(std::lround(haptic::FUNNEL_EXPONENT*100));
    const uint16_t span = uint16_t(std::lround(haptic::FUNNEL_DISTANCE_MAX*100));
    uint8_t musicalValue[]={0x4d,1,3,1,uint8_t(exponent),uint8_t(exponent>>8),uint8_t(span),uint8_t(span>>8)};
    // v3/modelo 1; r y dmax expresados en centesimas.
    musicalCaps->setValue(musicalValue,sizeof(musicalValue));
    auto* levelCaps=service->createCharacteristic(LEVEL_CAPS_UUID,BLECharacteristic::PROPERTY_READ);
    uint8_t levelValue[]={0x49,1,4,1}; // v4: intensidad global despues de GAIN.
    levelCaps->setValue(levelValue,sizeof(levelValue));
    statusCccd = new BLE2902();
    statusCharacteristic->addDescriptor(statusCccd);
    uint8_t initial[haptic::FRAME_SIZE]; haptic::ack(engine.active, haptic::OK, initial);
    statusCharacteristic->setValue(initial, sizeof(initial));
    if (xTaskCreate(replyTask, "haptic_ack", 4096, nullptr, 1, nullptr) != pdPASS)
        fatal("FATAL: no se pudo crear la tarea ACK.");
    service->start();
    auto* advertising = BLEDevice::getAdvertising();
    advertising->addServiceUUID(SERVICE_UUID);
    advertising->setScanResponse(true);
    BLEDevice::startAdvertising();
    Serial.printf("\nVIBRAFIL DUAL v5.1 | GPIO%d=L GPIO%d=R | reset_reason=%d\n",
        MOTOR_LEFT_PIN, MOTOR_RIGHT_PIN, int(esp_reset_reason()));
    Serial.println("BLE: ESP32 Vibrafil | Dos salidas; sin DAC/I2S ni arranque de motores.");
    Serial.println("FREQ: la nota gobierna el PWM electrico (no mide la vibracion mecanica).");
    Serial.println("Escribe STATUS, TEST L, TEST R, PLAY DO, PLAY SI o PLAY MID y Enter.");
    printStatus();

}
void loop() {
    if (engine.syncSession(session.load())) {
        sweep.stop(); writeOutputs();
        logNonBlocking(connected.load() ? "[BLE] conectado; salidas OFF.\n" : "[BLE] desconectado; salidas OFF.\n");
    }
    if (engine.tick(millis())) writeOutputs();
    Pending p{};
    if (xQueueReceive(pendingQueue, &p, 0) == pdTRUE) applyCommand(p, true);
    serialCommands();
    haptic::Command nextNote;
    if (sweep.next(millis(), nextNote)) {
        Pending demo{}; demo.receivedAt=millis(); demo.command=nextNote; demo.result=haptic::OK;
        applyCommand(demo, false);
    }
    if (engine.tick(millis())) writeOutputs();
    if (advertiseAgain.exchange(false) && !connected.load()) BLEDevice::startAdvertising();
    delay(1); // Cede al planificador; nunca delay(duracionDeNota).
}
