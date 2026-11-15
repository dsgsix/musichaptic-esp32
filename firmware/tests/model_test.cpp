#include "HapticModel.h"
#include <cassert>
#include <cstdio>
#include <limits>
#include <initializer_list>

int main() {
    auto l = haptic::noteLevels(0, 100);
    auto r = haptic::noteLevels(11, 100);
    assert(l.left == 100 && l.right == 0);
    assert(r.left == 0 && r.right == 100);
    const auto middle = haptic::funnel(100, .5f);
    const float expectedMiddle = 100 * std::log(6.0f) / std::log(11.0f);
    assert(middle.left == middle.right);
    assert(std::fabs(middle.left - expectedMiddle) < .0001f);
    float previousL = 101, previousR = -1;
    for (int i = 0; i < 12; ++i) {
        const auto levels = haptic::noteLevels(i, 100);
        assert(levels.left < previousL && levels.right > previousR);
        previousL = levels.left; previousR = levels.right;
        const auto mirror = haptic::noteLevels(11 - i, 100);
        assert(std::fabs(levels.left - mirror.right) < .0001f);
        if (i) assert(std::fabs(haptic::NOTE_HZ[i] / haptic::NOTE_HZ[i-1] - 1.22f) < .0001f);
        std::printf("pc=%2d Hz=%6.2f L=%6.2f R=%6.2f\n", i,
            double(haptic::noteFrequency(i)), double(levels.left), double(levels.right));
    }
    assert(haptic::noteFrequency(-1) == 0 && haptic::noteFrequency(12) == 0);
    for (float position : {-1.0f, 2.0f, std::numeric_limits<float>::quiet_NaN()}) {
        const auto levels = haptic::funnel(100, position);
        assert(levels.left == 0 && levels.right == 0);
    }
    assert(haptic::funnel(100, .5f, 0).left == 0);
    assert(haptic::funnel(100, .5f, 1, 0).right == 0);
    assert(haptic::funnel(0, .5f).left == 0);
    assert(std::fabs(haptic::funnel(50, .25f).left * 2 - haptic::funnel(100, .25f).left) < .001f);
    std::puts("PASS: supplied frequencies, Weber ratios, logarithmic funnel, midpoint, bounds.");
}
