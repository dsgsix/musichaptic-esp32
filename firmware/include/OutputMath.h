#pragma once
#include <cmath>
#include <cstdint>
namespace haptic {
struct OutputLevels { uint8_t left = 0, right = 0; };
// Escala ambos lados por el MISMO factor: no saturar cada uno por separado,
// pues eso destruiria el reparto espacial (dos valores altos se volverian 100/100).
inline OutputLevels sideLevels(uint8_t left, uint8_t right, uint16_t gain, uint8_t intensity = 100) {
    OutputLevels out;
    if (left > 100 || right > 100 || gain > 300 || intensity > 100) return out;
    const uint8_t maximum = left > right ? left : right;
    if (!maximum) return out;
    // Aplica la ganancia al patron espacial antes de la intensidad global.
    // Conserva los niveles 25/60/100 incluso cuando la ganancia satura el patron.
    const float scale = std::fmin(gain / 100.0f, 100.0f / maximum) * intensity / 100.0f;
    out.left = uint8_t(std::lround(left * scale));
    out.right = uint8_t(std::lround(right * scale));
    return out;
}
} // namespace haptic
