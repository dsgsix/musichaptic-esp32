#include "HapticEngine.h"
#include "OutputMath.h"
#include "ModulePwm.h"
#include <cassert>
#include <cstdio>
#include <initializer_list>
namespace fake {
uint32_t writes[2]={},divider=0; int pins[2]={},bound[2]={-1,-1}; bool failTimer=false;
}
int main() {
    using namespace haptic;
    ModulePwm pwm; assert(pwm.begin() && pwm.frequencyMode());
    Engine engine;
    for(int pc=0;pc<12;pc++) for(int gain : {100,125,150,300}) {
        uint32_t previous=0;
        for(int master : {25,60,100}) {
            Command c; notePan(pc,100,c); c.version=4; c.intensity=master; c.duration=500;
            uint8_t wire[16]; ack(c,OK,wire); wire[0]=0x48; wire[2]=1; wire[15]=checksum(wire,16);
            Command parsed; assert(decode(wire,16,parsed)==OK && parsed.intensity==master);
            assert(engine.apply(parsed,OK,100,100)==OK);
            auto levels=sideLevels(parsed.left,parsed.right,gain,parsed.intensity);
            assert(levels.left<=master && levels.right<=master);
            assert(pwm.set(levels.left,levels.right,parsed.carrierCentiHz));
            const uint32_t maximum=fake::writes[0]>fake::writes[1]?fake::writes[0]:fake::writes[1];
            assert(maximum>previous && maximum<=1535); previous=maximum;
            assert(std::fabs(pwm.actualHz()-NOTE_HZ[pc])<.02f);
            wire[14]=101;wire[15]=checksum(wire,16);
            assert(decode(wire,16,parsed)==BAD_VALUE);
            assert(engine.apply(parsed,BAD_VALUE,100,100)==BAD_VALUE && !engine.running);
            uint8_t reply[16]; assert(ack(parsed,BAD_VALUE,reply)==16);
            for(int i=5;i<15;i++) assert(reply[i]==0);
        }
    }
    assert(sideLevels(100,0,150,25).left==25);
    assert(sideLevels(100,0,150,60).left==60);
    assert(sideLevels(100,0,150,100).left==100);
    assert(sideLevels(100,100,150,0).left==0);
    uint8_t stop[16]={0x48,4,0}; stop[15]=checksum(stop,16);
    Command c; assert(decode(stop,16,c)==OK && c.intensity==0);
    assert(engine.apply(c,OK,0,0)==OK && !engine.running);
    std::puts("PASS: v4 wire validation, 12 frequencies x 4 gains x 3 distinct PWM levels, muted/invalid OFF and ACK.");
}
