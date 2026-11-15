#pragma once
#include "HapticProtocol.h"
namespace haptic {
// Estado puro, probado en PC. Solo loop() modifica este objeto y el hardware.
class Engine {
public:
    Command active{};
    uint32_t startedAt = 0;
    bool running = false;
    void stop() { active = Command{}; running = false; }
    Result apply(const Command& c, Result result, uint32_t receivedAt, uint32_t now) {
        stop();
        if (result != OK) return result;
        if (c.left > 100 || c.right > 100 || c.duration > MAX_DURATION_MS || c.center > 100 || c.intensity > 100) return BAD_VALUE;
        const uint16_t maximumHz = c.version >= 3 ? 600 : MAX_CENTER_HZ;
        if (c.carrierCentiHz && (c.carrierCentiHz < 6000 || c.carrierCentiHz > 60000)) return BAD_VALUE;
        if (c.center && (c.centerHz < MIN_CENTER_HZ || c.centerHz > maximumHz)) return BAD_VALUE;
        if (!c.center && c.centerHz) return BAD_VALUE;
        if (c.center) return CENTER_UNAVAILABLE;
        if ((c.left == 0 && c.right == 0 && c.center == 0) || c.intensity == 0) return OK;
        if (c.duration == 0) return BAD_VALUE;
        // El plazo comienza al recibir la orden: no reproducir una nota ya vencida.
        if (expired(now, receivedAt, c.duration)) return STALE_COMMAND;
        active = c; startedAt = receivedAt; running = true;
        return OK;
    }
    bool tick(uint32_t now) {
        if (!running || !expired(now, startedAt, active.duration)) return false;
        stop(); return true;
    }
    bool syncSession(uint32_t value) {
        if (value == session_) return false;
        session_ = value; stop(); return true;
    }
private:
    uint32_t session_ = 0;
};
} // namespace haptic
