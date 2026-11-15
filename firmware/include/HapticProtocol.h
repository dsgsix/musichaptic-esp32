#pragma once
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include "HapticModel.h"

namespace haptic {
constexpr uint16_t MAX_DURATION_MS = 3000;
constexpr size_t FRAME_SIZE = 10; // legacy v1
constexpr size_t FRAME_SIZE_V2 = 13;
constexpr size_t FRAME_SIZE_V3 = 15;
constexpr size_t FRAME_SIZE_V4 = 16;
constexpr size_t MAX_FRAME_SIZE = FRAME_SIZE_V4;
constexpr uint16_t MIN_CENTER_HZ = 20, MAX_CENTER_HZ = 500;
constexpr size_t MAX_TEXT_SIZE = 95;
struct Command {
    uint16_t sequence = 0;
    uint8_t left = 0, right = 0;
    uint16_t duration = 0;
    uint8_t center = 0, version = 1;
    uint16_t centerHz = 0;
    uint16_t carrierCentiHz = 0; // v3: exact requested note carrier in hundredths of Hz.
    uint8_t intensity = 100; // v4: master level AFTER hardware gain, independent of spatial levels.
};
enum Result : uint8_t { OK = 0, BAD_FRAME = 1, BAD_VALUE = 2, STALE_COMMAND = 3, CENTER_UNAVAILABLE = 4 };
inline size_t frameSize(uint8_t version) { return version == 4 ? FRAME_SIZE_V4 : version == 3 ? FRAME_SIZE_V3 : version == 2 ? FRAME_SIZE_V2 : FRAME_SIZE; }
inline uint8_t checksum(const uint8_t* p, size_t n = FRAME_SIZE) {
    uint8_t x = 0;
    for (size_t i = 0; i + 1 < n; ++i) x ^= p[i];
    return x;
}
inline Result decode(const uint8_t* p, size_t n, Command& out) {
    out = Command{};
    if (!p || n < 5) return BAD_FRAME;
    out.version = p[1] == 4 ? 4 : p[1] == 3 ? 3 : p[1] == 2 ? 2 : 1;
    out.sequence = p[3] | (uint16_t(p[4]) << 8);
    if ((p[1] != 1 && p[1] != 2 && p[1] != 3 && p[1] != 4) || n != frameSize(out.version)
        || p[0] != 0x48 || checksum(p,n) != p[n-1]) return BAD_FRAME;
    const bool v2 = out.version >= 2;
    const uint8_t center = v2 ? p[7] : 0;
    const uint16_t hz = v2 ? p[8] | (uint16_t(p[9]) << 8) : 0;
    const size_t d = v2 ? 10 : 7;
    const uint16_t ms = p[d] | (uint16_t(p[d+1]) << 8);
    if (p[2] > 1 || p[5] > 100 || p[6] > 100 || center > 100
        || ms > MAX_DURATION_MS || (p[2] == 1 && ms == 0)) return BAD_VALUE;
    const uint16_t carrier = out.version >= 3 ? p[12] | (uint16_t(p[13]) << 8) : 0;
    const uint16_t maximumHz = out.version >= 3 ? 600 : MAX_CENTER_HZ;
    const uint8_t intensity = out.version == 4 ? p[14] : 100;
    if (intensity > 100) return BAD_VALUE;
    if (center ? (hz < MIN_CENTER_HZ || hz > maximumHz) : hz != 0) return BAD_VALUE;
    if (out.version >= 3 && p[2] == 1 && (carrier < 6000 || carrier > 60000)) return BAD_VALUE;
    if (out.version >= 3 && center && hz != uint16_t((uint32_t(carrier)+50)/100)) return BAD_VALUE;
    if (p[2] == 0) {
        if (out.version == 4) out.intensity = 0;
        return (p[5] || p[6] || center || hz || ms || carrier || (out.version == 4 && intensity)) ? BAD_VALUE : OK;
    }
    out.carrierCentiHz = carrier;
    out.intensity = intensity;
    out.left=p[5]; out.right=p[6]; out.center=center; out.centerHz=hz; out.duration=ms;
    return OK;
}
// Callers must allocate frameSize(c.version) bytes (at most MAX_FRAME_SIZE).
inline size_t ack(const Command& c, Result result, uint8_t* p) {
    const size_t n=frameSize(c.version), d=c.version>=2 ? 10 : 7;
    std::memset(p,0,n);
    p[0]=0x41; p[1]=c.version; p[2]=result;
    p[3]=c.sequence & 255; p[4]=c.sequence >> 8;
    if(result==OK) {
        p[5]=c.left; p[6]=c.right;
        if(c.version>=2) { p[7]=c.center; p[8]=c.centerHz & 255; p[9]=c.centerHz >> 8; }
        if(c.version>=3) { p[12]=c.carrierCentiHz & 255; p[13]=c.carrierCentiHz >> 8; }
        if(c.version==4) p[14]=c.intensity;
        p[d]=c.duration & 255; p[d+1]=c.duration >> 8;
    }
    p[n-1]=checksum(p,n); return n;
}
inline void pan(float intensity, float position, Command& c) {
    const float angle = position * 0.0157079632679f;
    c.left = position == 100 ? 0 : uint8_t(std::lround(intensity * std::cos(angle)));
    c.right = position == 0 ? 0 : uint8_t(std::lround(intensity * std::sin(angle)));
}
inline bool number(const char* s, float& value) {
    if (!s || !*s) return false;
    // Decimal ASCII, sin espacios, NaN/Inf, hexadecimal ni notacion cientifica.
    bool digit = false, dot = false;
    for (const char* p = s; *p; ++p) {
        if (*p >= '0' && *p <= '9') digit = true;
        else if (*p == '.' && !dot) dot = true;
        else return false;
    }
    if (!digit) return false;
    char* end = nullptr;
    value = std::strtof(s, &end);
    return end != s && *end == 0 && std::isfinite(value);
}
inline bool durationValue(const char* s, uint16_t& ms) {
    float f;
    if (!number(s, f) || f < 1 || f > MAX_DURATION_MS || f != std::floor(f)) return false;
    ms = uint16_t(f); return true;
}
// Do/C=0 ... Si/B=11. Admite sostenido/bemol ASCII y octava opcional -1, 0..9.
// Ejemplos: DO, C4, RE#5, Db3, SIb. La octava no cambia el lado.
inline int pitchClass(const char* name) {
    if (!name || !*name || std::strlen(name) > 8) return -1;
    char s[9]; size_t n = std::strlen(name);
    for (size_t i = 0; i <= n; ++i) {
        char c = name[i]; s[i] = (c >= 'a' && c <= 'z') ? char(c - 'a' + 'A') : c;
    }
    const char* solfege[] = {"DO", "RE", "MI", "FA", "SOL", "LA", "SI"};
    const int pcs[] = {0, 2, 4, 5, 7, 9, 11};
    int pc = -1; size_t used = 0;
    for (size_t i = 0; i < 7; ++i) {
        const size_t len = std::strlen(solfege[i]);
        if (!std::strncmp(s, solfege[i], len)) { pc = pcs[i]; used = len; break; }
    }
    if (pc < 0) {
        const char* letters = "CDEFGAB";
        const char* p = std::strchr(letters, s[0]);
        if (!p) return -1;
        pc = pcs[p - letters]; used = 1;
    }
    if (s[used] == '#') { pc = (pc + 1) % 12; ++used; }
    else if (s[used] == 'B') { pc = (pc + 11) % 12; ++used; }
    if (s[used] == 0) return pc;
    if (s[used] >= '0' && s[used] <= '9' && s[used + 1] == 0) return pc;
    if (s[used] == '-' && s[used + 1] == '1' && s[used + 2] == 0) return pc;
    return -1;
}
inline void notePan(int pc, float intensity, Command& c) {
    const auto levels = noteLevels(pc, intensity);
    c.left = uint8_t(std::lround(levels.left)); c.right = uint8_t(std::lround(levels.right));
    c.carrierCentiHz = uint16_t(std::lround(noteFrequency(pc) * 100.0f));
    c.version = 3;
}
// Un comando completo por escritura GATT (no se ensamblan fragmentos).
// Legacy: NOTA|HZ|INTENSIDAD|POSICION[|MS]. HZ se valida, no gobierna el LRA.
// NOTE|DO4|80|500 = mapeo automatico. LR|80|20|500 = porcentajes directos.
inline Result decodeText(const char* text, size_t n, Command& out, bool routeByNote = true) {
    out = Command{};
    if (!text || n == 0 || n > MAX_TEXT_SIZE) return BAD_FRAME;
    char buf[MAX_TEXT_SIZE + 1]; std::memcpy(buf, text, n); buf[n] = 0;
    if (std::memchr(buf, 0, n)) return BAD_FRAME;
    while (n && (buf[n-1] == '\r' || buf[n-1] == '\n')) buf[--n] = 0;
    if (!std::strcmp(buf, "STOP")) return OK;
    if (!std::strcmp(buf, "TEST C") || !std::strcmp(buf, "TEST ALL")) {
        out.version=2; out.center=20; out.centerHz=160; out.duration=500;
        if (!std::strcmp(buf, "TEST ALL")) { out.left=80; out.right=80; }
        return OK;
    }
    if (!std::strcmp(buf, "TEST L") || !std::strcmp(buf, "TEST R") || !std::strcmp(buf, "TEST B")) {
        out.duration = 500;
        out.left = buf[5] == 'R' ? 0 : 80;
        out.right = buf[5] == 'L' ? 0 : 80;
        return OK;
    }
    char* fields[5] = {buf}; int count = 1;
    for (size_t i = 0; i < n; ++i) if (buf[i] == '|') {
        if (count == 5) return BAD_FRAME;
        buf[i] = 0; fields[count++] = buf + i + 1;
    }
    if (count != 4 && count != 5) return BAD_FRAME;
    if (!std::strcmp(fields[0], "CENTER")) {
        if(count!=4) return BAD_FRAME;
        float level,hz;
        if(!number(fields[1],level) || !number(fields[2],hz) || level>100
            || level!=std::floor(level) || hz!=std::floor(hz)
            || (level>0 ? (hz<MIN_CENTER_HZ || hz>MAX_CENTER_HZ) : hz!=0)
            || !durationValue(fields[3],out.duration)) return BAD_VALUE;
        out.version=2; out.center=uint8_t(level); out.centerHz=uint16_t(hz); return OK;
    }
    if (!std::strcmp(fields[0], "FUNNEL")) {
        if (count != 4) return BAD_FRAME;
        float position, intensity;
        if (!number(fields[1], position) || position > 100 || !number(fields[2], intensity) || intensity > 100 ||
            !durationValue(fields[3], out.duration)) return BAD_VALUE;
        const auto levels = funnel(intensity, position / 100.0f);
        out.left = uint8_t(std::lround(levels.left)); out.right = uint8_t(std::lround(levels.right));
        const float index = position * 11.0f / 100.0f;
        const int lower = int(index); const int upper = lower < 11 ? lower + 1 : 11;
        const float hz = NOTE_HZ[lower] * std::pow(NOTE_HZ[upper]/NOTE_HZ[lower], index-lower);
        out.carrierCentiHz = uint16_t(std::lround(hz*100)); out.version=3; return OK;
    }
    if (!std::strcmp(fields[0], "NOTE")) {
        if (count != 4 && count != 5) return BAD_FRAME;
        const int pc = pitchClass(fields[1]); float intensity;
        if (pc < 0 || !number(fields[2], intensity) || intensity > 100 ||
            !durationValue(fields[3], out.duration)) return BAD_VALUE;
        notePan(pc, intensity, out);
        if (count == 5) {
            float gain;
            if (!number(fields[4],gain) || gain>100) return BAD_VALUE;
            out.center=uint8_t(std::lround((out.left>out.right ? out.left : out.right)*gain/100));
            out.centerHz=out.center ? uint16_t(std::lround(out.carrierCentiHz/100.0f)) : 0;
        }
        return OK;
    }
    if (!std::strcmp(fields[0], "LR")) {
        if (count != 4) return BAD_FRAME;
        float left, right;
        if (!number(fields[1], left) || !number(fields[2], right) || left > 100 || right > 100 ||
            left != std::floor(left) || right != std::floor(right) ||
            !durationValue(fields[3], out.duration)) return BAD_VALUE;
        out.left = uint8_t(left); out.right = uint8_t(right); return OK;
    }
    if (!fields[0][0] || std::strlen(fields[0]) > 8) return BAD_VALUE;
    float hz, intensity, position;
    uint16_t ms = 350;
    if (!number(fields[1], hz) || !number(fields[2], intensity) || !number(fields[3], position)
        || (count == 5 && !durationValue(fields[4], ms))) return BAD_VALUE;
    if (hz > 20000 || intensity > 100 || position > 100) return BAD_VALUE;
    const int pc = pitchClass(fields[0]);
    if (routeByNote && pc >= 0) notePan(pc, intensity, out);
    else pan(intensity, position, out);
    out.duration = ms;
    return OK;
}
inline bool expired(uint32_t now, uint32_t started, uint16_t duration) {
    return uint32_t(now - started) >= duration;
}
inline uint32_t duty(uint8_t percent, bool activeHigh) {
    const uint32_t d = (uint32_t(percent) * 255 + 50) / 100;
    return activeHigh ? d : 255 - d;
}
} // namespace haptic
