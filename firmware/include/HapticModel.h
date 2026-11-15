#pragma once
#include <cmath>
#include <cstdint>

namespace haptic {
// Frecuencias hapticas de referencia; razon aproximada entre notas: 1.22.
constexpr float WEBER_FRACTION = 0.22f;
constexpr float NOTE_HZ[12] = {
    62.15f, 75.82f, 92.50f, 112.85f, 137.68f, 167.97f,
    204.92f, 250.00f, 305.00f, 372.10f, 453.96f, 553.83f
};
// Las coordenadas espaciales de Android van de 0 a 10.
// El exponente r es independiente de Weber; r=1 requiere calibracion fisica.
constexpr float FUNNEL_DISTANCE_MAX = 10.0f;
constexpr float FUNNEL_EXPONENT = 1.0f;
struct Levels { float left = 0, right = 0; };

inline Levels funnel(float intensity, float position,
                     float exponent = FUNNEL_EXPONENT,
                     float distanceMax = FUNNEL_DISTANCE_MAX) {
    Levels result;
    if (!std::isfinite(intensity) || !std::isfinite(position) ||
        !std::isfinite(exponent) || !std::isfinite(distanceMax) ||
        intensity < 0 || intensity > 100 || position < 0 || position > 1 ||
        exponent <= 0 || distanceMax <= 0) return result;
    const float denominator = std::log1p(distanceMax);
    const float d = position * distanceMax;
    // Modelo espacial logaritmico, simetrico respecto al punto medio.
    // El canal derecho aumenta con d; r=1 queda pendiente de calibracion.
    result.right = intensity * std::pow(std::log1p(d) / denominator, exponent);
    result.left = intensity * std::pow(std::log1p(distanceMax - d) / denominator, exponent);
    return result;
}
inline float noteFrequency(int pitchClass) {
    return pitchClass >= 0 && pitchClass < 12 ? NOTE_HZ[pitchClass] : 0;
}
inline Levels noteLevels(int pitchClass, float intensity) {
    return pitchClass >= 0 && pitchClass < 12
        ? funnel(intensity, float(pitchClass) / 11.0f) : Levels{};
}
} // namespace haptic
