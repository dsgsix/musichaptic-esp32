#pragma once
#include <cstdint>
#include "HapticConfig.h"
namespace haptic {
// APB / ((divisor/256) * 2^11); redondeo al divisor fraccional mas cercano.
inline uint32_t pwmDivider(uint32_t clockHz, uint32_t centiHz) {
    if (!centiHz) return 0;
    return uint32_t((uint64_t(clockHz) * 100 + uint64_t(centiHz) * 4) /
                    (uint64_t(centiHz) * 8));
}
inline float pwmFrequency(uint32_t clockHz, uint32_t divider) {
    return divider ? float(clockHz) / (float(divider) * 8.0f) : 0;
}
inline uint32_t moduleDuty(uint8_t level, bool frequencyMode, bool activeHigh,
                           uint8_t frequencyDutyPercent = 50) {
    if (level > 100) level = 0;
    // POWER: notas y TEST usan exactamente la misma escala 0..100 %.
    // FREQ: el maximo es configurable (por defecto historico: 50 %).
    // Se limita a 50..90 %: 100 % seria nivel continuo y eliminaria la
    // alternancia electrica que permite conservar la frecuencia solicitada.
    if (frequencyDutyPercent < MIN_FREQUENCY_DUTY_PERCENT)
        frequencyDutyPercent = MIN_FREQUENCY_DUTY_PERCENT;
    if (frequencyDutyPercent > MAX_FREQUENCY_DUTY_PERCENT)
        frequencyDutyPercent = MAX_FREQUENCY_DUTY_PERCENT;
    const uint32_t maximum = frequencyMode
        ? (2047u * uint32_t(frequencyDutyPercent) + 50u) / 100u
        : 2047u;
    const uint32_t duty = (uint32_t(level) * maximum + 50) / 100;
    return activeHigh ? duty : 2047 - duty;
}
} // namespace haptic
