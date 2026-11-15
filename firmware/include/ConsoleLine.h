#pragma once
#include <cstdint>
#include <cstddef>
#include "HapticProtocol.h"
namespace haptic {
class ConsoleLine {
public:
    enum class Event { None, Ready, Invalid };
    Event feed(uint8_t c) {
        if (c == '\r') return Event::None;
        if (c == '\n') {
            if (invalid_) { reset(); return Event::Invalid; }
            size_t out = 0;
            bool space = false;
            for (size_t i = 0; i < length_; ++i) {
                uint8_t ch = uint8_t(buffer_[i]);
                if (ch == ' ' || ch == '\t') { space = out != 0; continue; }
                if (space) buffer_[out++] = ' ';
                space = false;
                if (ch >= 'a' && ch <= 'z') ch -= ('a' - 'A');
                buffer_[out++] = char(ch);
            }
            buffer_[out] = 0;
            length_ = 0;
            return out ? Event::Ready : Event::None;
        }
        if (c == 8 || c == 127) { if (length_) --length_; return Event::None; }
        // No convertir secuencias ESC o bytes desconocidos en una orden de actuacion.
        if ((c < 32 && c != '\t') || c > 126) invalid_ = true;
        if (!invalid_) {
            if (length_ < sizeof(buffer_) - 1) buffer_[length_++] = char(c);
            else invalid_ = true;
        }
        return Event::None;
    }
    const char* text() const { return buffer_; }
private:
    void reset() { length_ = 0; invalid_ = false; buffer_[0] = 0; }
    char buffer_[MAX_TEXT_SIZE + 1]{};
    size_t length_ = 0;
    bool invalid_ = false;
};

}
