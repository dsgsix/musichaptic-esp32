#pragma once
#include <cstdint>
constexpr int OUTPUT=1, LOW=0, HIGH=1;
namespace fake {
extern uint32_t writes[2], divider;
extern int pins[2], bound[2];
extern bool failTimer;
}
inline void pinMode(int,int) {}
inline void digitalWrite(int,int) {}
inline uint32_t ledcSetup(uint8_t,uint32_t hz,uint8_t) { return hz; }
inline void ledcAttachPin(int pin,uint8_t channel) { fake::pins[channel]=pin; }
inline void ledcWrite(uint8_t channel,uint32_t duty) { fake::writes[channel]=duty; }
inline uint32_t getApbFrequency() { return 80000000; }
