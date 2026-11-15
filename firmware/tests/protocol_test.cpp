#include "HapticEngine.h"
#include <cassert>
#include <cstdio>
#include <random>
#include <vector>
static haptic::Result text(const char* s, haptic::Command& c, bool byNote = true) {
    return haptic::decodeText(s, std::strlen(s), c, byNote);
}
int main() {
    haptic::Command c;
    assert(text("DO|261.63|80|0|500",c)==haptic::OK && c.left==80 && c.right==0 && c.duration==500);
    assert(text("SI|493.88|80|100",c)==haptic::OK && c.left==0 && c.right==80 && c.duration==350);
    // Regression: an app sending the same position for every note no longer defeats note routing.
    assert(text("DO|261.63|80|50",c)==haptic::OK && c.left==80 && c.right==0);
    assert(text("SI|493.88|80|50",c)==haptic::OK && c.left==0 && c.right==80);
    // Compatibility switch and explicit PAN preserve user-chosen position.
    assert(text("DO|261.63|80|50",c,false)==haptic::OK && c.left==57 && c.right==57);
    assert(text("PAN|261.63|80|50",c)==haptic::OK && c.left==57 && c.right==57);
    assert(text("TEST L",c)==haptic::OK && c.left==80 && c.right==0);
    assert(text("TEST R",c)==haptic::OK && c.left==0 && c.right==80);
    assert(text("TEST B",c)==haptic::OK && c.left==80 && c.right==80);
    assert(text("STOP\r\n",c)==haptic::OK && c.left==0 && c.right==0 && c.duration==0);
    assert(text("LR|70|30|1000",c)==haptic::OK && c.left==70 && c.right==30 && c.duration==1000);
    assert(text("NOTE|do4|80|500",c)==haptic::OK && c.left==80 && c.right==0);
    assert(text("NOTE|B-1|80|500",c)==haptic::OK && c.left==0 && c.right==80);
    assert(text("NOTE|Db3|80|500",c)==haptic::OK && c.left>c.right && c.right>0);
    const char* notes[] = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
    int prevLeft=101, prevRight=-1;
    for (int i=0;i<12;++i) {
        assert(haptic::pitchClass(notes[i])==i);
        haptic::notePan(i,100,c);
        assert(c.left<=prevLeft && c.right>=prevRight);
        assert(c.carrierCentiHz == uint16_t(std::lround(haptic::NOTE_HZ[i]*100)));
        prevLeft=c.left; prevRight=c.right;
    }
    assert(haptic::pitchClass("SIb")==10 && haptic::pitchClass("Cb")==11);
    assert(haptic::pitchClass("DO#9")==1 && haptic::pitchClass("SOLb4")==6);
    for (const char* s: {"", "DO|x|80|0", "DO|261|nan|0", "SI|493|80|inf", "DO|261|80|0|3001",
        "DO|261|80|0|0", "DO|261|80|0|-1", "DO|261|80|0|1.5", "DO|261|80|101", "DO|261|101|0",
        "DO|261||0", "DO|261|80|0|500|1", "DO|261|80|0|500junk", "NOTE|nonsense|80|500",
        "NOTE|DO|80|500|101", "LR|nan|0|500", "LR|10.5|0|500", "LR|0|-1|500", "NOTE|DO10|80|500",
        "NOTE|DO-|80|500", "NOTE|DO-2|80|500", "NOTE|DO| 80|500", "NOTE|DO|0x10|500"})
        assert(text(s,c)!=haptic::OK);
    assert(haptic::decodeText(nullptr,10,c)==haptic::BAD_FRAME);
    assert(haptic::decode(nullptr,10,c)==haptic::BAD_FRAME);
    const char nul[]={'S','T','O','P',0,'X'};
    assert(haptic::decodeText(nul,sizeof(nul),c)==haptic::BAD_FRAME);
    std::vector<uint8_t> b={0x48,1,1,0x34,0x12,80,0,0xf4,1,0xcb};
    assert(haptic::decode(b.data(),b.size(),c)==haptic::OK);
    assert(c.sequence==0x1234 && c.left==80 && c.right==0 && c.duration==500);
    for (size_t i=0;i<b.size();++i) for (int bit=0;bit<8;++bit) {
        b[i]^=1<<bit; assert(haptic::decode(b.data(),b.size(),c)!=haptic::OK); b[i]^=1<<bit;
    }
    uint8_t reply[10]; haptic::decode(b.data(),b.size(),c); haptic::ack(c,haptic::OK,reply);
    assert(reply[0]==0x41 && reply[2]==0 && reply[5]==80 && reply[9]==haptic::checksum(reply));
    haptic::ack(c,haptic::STALE_COMMAND,reply);
    assert(reply[2]==3 && reply[5]==0 && reply[7]==0 && reply[9]==haptic::checksum(reply));
    b[5]=101; b[9]=haptic::checksum(b.data()); assert(haptic::decode(b.data(),b.size(),c)==haptic::BAD_VALUE);
    b[5]=80; b[7]=0xb9; b[8]=0x0b; b[9]=haptic::checksum(b.data());
    assert(haptic::decode(b.data(),b.size(),c)==haptic::BAD_VALUE);
    b={0x48,1,0,0xff,0xff,0,0,0,0,0}; b[9]=haptic::checksum(b.data());
    assert(haptic::decode(b.data(),b.size(),c)==haptic::OK && c.duration==0 && c.sequence==65535);
    b[5]=1; b[9]=haptic::checksum(b.data()); assert(haptic::decode(b.data(),b.size(),c)==haptic::BAD_VALUE);
    // Stateful regression: alternating and simultaneous outputs, preemption, invalid input,
    // stop, timeout, delayed mailbox, reconnect and millis rollover.
    haptic::Engine e;
    text("TEST L",c); assert(e.apply(c,haptic::OK,100,100)==haptic::OK);
    assert(e.active.left==80 && e.active.right==0 && e.running);
    text("TEST R",c); e.apply(c,haptic::OK,110,110);
    assert(e.active.left==0 && e.active.right==80);
    text("TEST B",c); e.apply(c,haptic::OK,120,120);
    assert(e.active.left==80 && e.active.right==80);
    assert(!e.tick(619)); assert(e.tick(620)); assert(!e.running && e.active.left==0 && e.active.right==0);
    e.apply(c,haptic::OK,1000,1499); assert(e.running); assert(e.tick(1500));
    assert(e.apply(c,haptic::OK,1000,1500)==haptic::STALE_COMMAND && !e.running);
    e.apply(c,haptic::OK,2000,2000); e.syncSession(1); assert(!e.running);
    e.apply(c,haptic::OK,2010,2010); assert(!e.syncSession(1) && e.running);
    e.syncSession(2); assert(!e.running);
    e.apply(c,haptic::OK,2020,2020); e.apply(c,haptic::BAD_FRAME,2021,2021); assert(!e.running);
    e.apply(c,haptic::OK,2030,2030); text("STOP",c); e.apply(c,haptic::OK,2031,2031); assert(!e.running);
    text("LR|0|0|500",c); assert(e.apply(c,haptic::OK,2040,2040)==haptic::OK && !e.running);
    text("LR|70|20|50",c); e.apply(c,haptic::OK,0xfffffff0u,0xfffffff0u);
    assert(!e.tick(0x10u)); assert(e.tick(0x22u));
    assert(haptic::duty(0,true)==0 && haptic::duty(100,true)==255 && haptic::duty(80,true)==204);
    assert(haptic::duty(0,false)==255 && haptic::duty(100,false)==0);
    for(int i=0;i<=100;++i) assert(haptic::duty(i,true)+haptic::duty(i,false)==255);
    std::mt19937 gen(21);
    for (int k=0;k<10000;k++) {
        uint8_t input[110]; for (auto& v:input) v=uint8_t(gen());
        size_t n=gen()%111;
        auto r=haptic::decode(input,n,c);
        if (r==haptic::OK) assert(c.left<=100 && c.right<=100 && c.duration<=3000);
        r=haptic::decodeText(reinterpret_cast<char*>(input),n,c);
        if (r==haptic::OK) assert(c.left<=100 && c.right<=100 && c.duration<=3000);
    }
    std::puts("PASS: protocol, note routing, dual engine, cutoff, reconnect state, stale packets, rollover, 10000 fuzz inputs.");
}
