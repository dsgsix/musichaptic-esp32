#include "HapticEngine.h"
#include "BenchCommands.h"
#include "OutputMath.h"
#include "ModulePwm.h"
#include "ConsoleLine.h"
#include <cassert>
#include <cstdio>
#include <string>
namespace fake {
uint32_t writes[2]={}, divider=0;
int pins[2]={},bound[2]={-1,-1}; bool failTimer=false;
}
static haptic::ConsoleLine::Event feed(haptic::ConsoleLine& console, const std::string& text) {
    auto event=haptic::ConsoleLine::Event::None;
    for (unsigned char c : text) event=console.feed(c);
    return event;
}
int main() {
    using namespace haptic;
    ConsoleLine console;
    assert(feed(console,"  play\t do \r\n")==ConsoleLine::Event::Ready);
    assert(std::string(console.text())=="PLAY DO");
    assert(feed(console,"TEST X\bL\n")==ConsoleLine::Event::Ready);
    assert(std::string(console.text())=="TEST L");
    assert(feed(console,"play x\177si\n")==ConsoleLine::Event::Ready);
    assert(std::string(console.text())=="PLAY SI");
    assert(feed(console,"\033[A TEST L\n")==ConsoleLine::Event::Invalid);
    assert(feed(console,std::string(100,'X')+"\b\n")==ConsoleLine::Event::Invalid);
    assert(feed(console,"  \n")==ConsoleLine::Event::None);
    assert(feed(console,"status\n")==ConsoleLine::Event::Ready);
    assert(std::string(console.text())=="STATUS");
    ModulePwm pwm; assert(pwm.begin()); assert(pwm.frequencyMode()); pwm.setFrequencyMode(false);
    Engine engine; Command c;
    assert(benchPlay("DO",c)==OK && c.center==0 && c.carrierCentiHz==6215);
    assert(engine.apply(c,OK,100,100)==OK);
    auto levels=sideLevels(c.left,c.right,DEFAULT_SIDE_GAIN);
    assert(levels.left==100 && levels.right==0);
    assert(pwm.set(levels.left,levels.right,c.carrierCentiHz));
    const auto noteDuty=fake::writes[0];
    assert(decodeText("TEST L",6,c)==OK);
    levels=sideLevels(c.left,c.right,DEFAULT_SIDE_GAIN);
    assert(pwm.set(levels.left,levels.right,c.carrierCentiHz));
    assert(fake::writes[0]==noteDuty && noteDuty==2047);
    assert(benchPlay("SI",c)==OK && c.center==0);
    levels=sideLevels(c.left,c.right,DEFAULT_SIDE_GAIN);
    assert(levels.left==0 && levels.right==100);
    assert(benchPlay("MID",c)==OK && c.center==0);
    levels=sideLevels(c.left,c.right,DEFAULT_SIDE_GAIN);
    assert(levels.left==75 && levels.right==75);
    assert(benchPlay("INVALID",c)==BAD_VALUE && c.left==0 && c.center==0);
    assert(benchPlay(nullptr,c)==BAD_VALUE);
    // Boost never invents activity on an OFF channel, or independently clips the pan.
    for(int l=0;l<=100;++l) for(int r=0;r<=100;++r) for(int gain : {0,100,125,200,300}) {
        levels=sideLevels(l,r,gain);
        assert(levels.left<=100 && levels.right<=100);
        if(!l || !gain) assert(levels.left==0);
        if(!r || !gain) assert(levels.right==0);
        assert(std::abs(int(levels.left)*r-int(levels.right)*l)<=100);
    }
    assert(sideLevels(90,70,200).right==78);
    assert(sideLevels(60,0,125).left==75);
    // El ACK refleja la orden recibida, antes de aplicar ganancia al duty.
    for (uint16_t seq=0;seq<1000;++seq) {
        const uint8_t left=uint8_t(seq%101),right=uint8_t(100-left);
        uint8_t frame[]={0x48,1,1,uint8_t(seq),uint8_t(seq>>8),left,right,0xf4,1,0};
        frame[9]=checksum(frame);
        assert(decode(frame,sizeof(frame),c)==OK);
        const uint32_t now=0xffff0000u+uint32_t(seq)*250;
        assert(engine.apply(c,OK,now,now+1)==OK);
        levels=sideLevels(c.left,c.right,125);
        assert(pwm.set(levels.left,levels.right,c.carrierCentiHz));
        assert(!engine.tick(now+249));
        uint8_t reply[FRAME_SIZE]; assert(ack(c,OK,reply)==10);
        assert(reply[0]==0x41 && reply[1]==1 && reply[2]==0);
        assert(reply[3]==frame[3] && reply[4]==frame[4]);
        for (int i=5;i<9;++i) assert(reply[i]==frame[i]);
        assert(reply[9]==checksum(reply));
    }
    assert(engine.tick(engine.startedAt+500));
    assert(pwm.set(engine.active.left,engine.active.right,0));
    assert(fake::writes[0]==0 && fake::writes[1]==0);
    // Explicit-frequency v3 is still supported, with central channel disabled.
    benchPlay("SI",c); uint8_t frame[MAX_FRAME_SIZE];
    ack(c,OK,frame);frame[0]=0x48;frame[2]=1;frame[14]=checksum(frame,15);
    assert(decode(frame,15,c)==OK && c.carrierCentiHz==55383);
    assert(engine.apply(c,OK,0,0)==OK);
    c.center=20;c.centerHz=554;
    assert(engine.apply(c,OK,0,0)==CENTER_UNAVAILABLE && !engine.running);
    BenchSweep sweep;uint32_t now=0xfffffff0u;sweep.start(now);
    for(int i=0;i<13;++i) {
        assert(sweep.next(now,c) && c.center==0);
        assert(engine.apply(c,OK,now,now)==OK);
        assert(!sweep.next(now,c)); assert(!engine.tick(now+699));assert(engine.tick(now+700));
        now+=1200;
    }
    assert(!sweep.running());sweep.start(now);sweep.stop();assert(!sweep.next(now,c));
    std::puts("PASS: edited USB input, TEST/NOTE same full duty, default gain, 51005 level/gain cases, 1000 app frames and ACKs, continuous renewal, dual-only v3, sweep and rollover.");
}
