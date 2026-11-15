#pragma once
#include "HapticProtocol.h"
namespace haptic {
// Solo herramientas de banco USB; BLE mantiene su protocolo v1/v2/v3 intacto.
inline Result benchPlay(const char* note, Command& out) {
    if (!note) { out = Command{}; return BAD_VALUE; }
    Result result;
    if (!std::strcmp(note, "MID")) {
        const char* text = "FUNNEL|50|80|700";
        result = decodeText(text, std::strlen(text), out);
    } else {
        const int pc = pitchClass(note);
        out = Command{};
        if (pc < 0) return BAD_VALUE;
        notePan(pc, 80, out); out.duration = 700; result = OK;
    }
    return result;
}
class BenchSweep {
public:
    void start(uint32_t now) { running_ = true; index_ = 0; nextAt_ = now; }
    void stop() { running_ = false; }
    bool running() const { return running_; }
    bool next(uint32_t now, Command& out) {
        if (!running_ || int32_t(now - nextAt_) < 0) return false;
        const char* names[] = {"DO", "DO#", "RE", "RE#", "MI", "FA", "FA#", "SOL", "SOL#", "LA", "LA#", "SI", "MID"};
        benchPlay(names[index_++], out);
        if (index_ == 13) running_ = false;
        // 700 ms activos y 500 ms de pausa; una demora no acumula notas vencidas.
        nextAt_ = now + 1200;
        return true;
    }
private:
    bool running_ = false;
    unsigned index_ = 0;
    uint32_t nextAt_ = 0;
};
} // namespace haptic
