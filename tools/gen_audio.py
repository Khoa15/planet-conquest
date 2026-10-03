#!/usr/bin/env python3
"""Sinh âm thanh sci-fi nhẹ cho Planet Conquest (không bản quyền). Dùng: python3 tools/gen_audio.py
Ra app/src/main/res/raw/*.ogg (cần numpy + ffmpeg)."""
import os, subprocess, tempfile, wave
import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
rng = np.random.default_rng(7)

def t(d): return np.arange(int(SR * d)) / SR
def env(n, a=0.005, r=0.1):
    e = np.ones(n); na = max(1, int(a * SR)); nr = max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na); e[-nr:] *= np.linspace(1, 0, nr); return e
def sine(f, d, vol=1.0): tt = t(d); ph = 2*np.pi*np.cumsum(np.broadcast_to(f, tt.shape))/SR; return np.sin(ph) * vol
def sweep(f0, f1, d): return sine(np.linspace(f0, f1, int(SR*d)), d)
def bell(f, d, decay=6): tt = t(d); return (np.sin(2*np.pi*f*tt) + .3*np.sin(2*np.pi*f*2.01*tt)) * np.exp(-decay*tt)
def lp(x, k): return np.convolve(x, np.ones(k)/k, mode="same")
def notes(seq, step, f=bell, **kw):
    out = np.zeros(int(SR*(step*len(seq)+1.5)))
    for i, fr in enumerate(seq):
        if fr is None: continue
        s = f(fr, 1.2, **kw); o = int(i*step*SR); out[o:o+len(s)] += s
    return out

def write(name, x, vol=0.7):
    x = x / max(1e-9, np.abs(x).max()) * vol
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tf: p = tf.name
    with wave.open(p, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x*32767).astype("<i2").tobytes())
    os.makedirs(OUT, exist_ok=True)
    subprocess.run(["ffmpeg","-y","-loglevel","error","-i",p,"-c:a","vorbis","-strict","-2","-ac","2","-b:a","96k",os.path.join(OUT,name+".ogg")], check=True)
    os.unlink(p)

N = lambda s: 440*2**((s-9)/12)   # semitone từ C4: 0=C4
# ---------- SFX ----------
def fx(name, x, vol=.7): write(name, x, vol)
d=.09;  fx("sfx_click", sine(1200,d)*env(int(SR*d),.002,.07)*.6 + sine(1800,d)*env(int(SR*d),.002,.05)*.3)
d=.18;  fx("sfx_lasso", sweep(400,900,d)*env(int(SR*d),.01,.12), .5)
d=.22;  fx("sfx_move",  sweep(500,750,d)*env(int(SR*d),.01,.15), .5)
d=.3;   fx("sfx_attack",(sweep(900,200,d)+.4*lp(rng.standard_normal(int(SR*d)),12))*env(int(SR*d),.003,.25))
d=.12;  fx("sfx_point", bell(1400,d,25), .5)
d=.45;  fx("sfx_upgrade", notes([N(7),N(12),N(16),N(19)],.08,bell,decay=7)[:int(SR*d+SR*.4)], .7)
fx("sfx_capture", notes([N(0),N(4),N(7),N(12)],.1,bell,decay=5)[:int(SR*1.0)], .75)
fx("sfx_levelup", notes([N(12),N(16),N(19),N(24)],.09,bell,decay=4)[:int(SR*1.1)], .75)
fx("sfx_win",  notes([N(0),N(4),N(7),N(12),N(16),N(19),N(24)],.13,bell,decay=3)[:int(SR*2.2)], .8)
fx("sfx_lose", notes([N(7),N(3),N(0),N(-5)],.28,bell,decay=3)[:int(SR*2.2)], .75)

# ---------- Nhạc nền (loop mượt: đuôi đè về đầu) ----------
def pad(freqs, d, vol=.25):
    tt=t(d); x=sum(np.sin(2*np.pi*f*tt)+.5*np.sin(2*np.pi*f*1.003*tt) for f in freqs)
    e=np.minimum(np.minimum(tt/(d*.35),1), (d-tt)/(d*.35)); return x*e*vol/len(freqs)
def music(name, chords, arp_oct, bpm, bars_rep=2, arp_vol=.5, bass=True):
    beat=60/bpm; bar=beat*4; total=bar*len(chords)*bars_rep; tail=2.5
    x=np.zeros(int(SR*(total+tail)))
    for r in range(bars_rep):
        for i,ch in enumerate(chords):
            st=(r*len(chords)+i)*bar; o=int(st*SR)
            p=pad([N(s) for s in ch],bar+1.0); x[o:o+len(p)]+=p
            if bass:
                b=sine(N(ch[0]-24),bar+.5)*env(int(SR*(bar+.5)),.05,.8)*.25; x[o:o+len(b)]+=b
            for k in range(8):   # arpeggio nốt tám
                s=ch[[0,1,2,1,2,1,2,1][k%8]]+12*arp_oct
                nt=bell(N(s),1.4,decay=3.5)*arp_vol*(.7 if k%2 else 1); oo=o+int(k*beat/2*SR); x[oo:oo+len(nt)]+=nt[:len(x)-oo]
    L=int(SR*total); y=x[:L].copy(); tl=x[L:]; y[:len(tl)]+=tl   # gập đuôi vào đầu
    # echo nhẹ
    dl=int(SR*beat*.75); y2=y.copy(); y2[dl:]+=.3*y[:-dl]; write(name,y2,.55)
music("bgm_menu", [[0,7,11,16],[-3,4,9,14],[-7,0,5,9],[-5,2,7,11]], 1, 70, 2, .45)
music("bgm_game", [[-3,4,7,12],[-7,0,4,9],[-4,3,7,10],[-5,2,7,11]], 1, 96, 2, .55)
print("done")
