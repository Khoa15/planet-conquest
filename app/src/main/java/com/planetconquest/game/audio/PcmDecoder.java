package com.planetconquest.game.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Giải mã một file âm thanh nén (OGG/Opus trong res/raw) thành PCM 16-bit mono để lặp vòng bằng AudioTrack. */
final class PcmDecoder {
    /** Kết quả giải mã: PCM 16-bit little-endian mono và tần số lấy mẫu. */
    static final class Pcm {
        final byte[] data;
        final int sampleRate;
        Pcm(byte[] data, int sampleRate) { this.data = data; this.sampleRate = sampleRate; }
    }

    private static final long TIMEOUT_US = 5_000;

    private final Context ctx;

    PcmDecoder(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    /** Chạy được trên luồng nền; ném IOException nếu không giải mã được. */
    Pcm decode(int rawRes) throws IOException {
        MediaExtractor ex = new MediaExtractor();
        MediaCodec codec = null;
        AssetFileDescriptor fd = ctx.getResources().openRawResourceFd(rawRes);
        try {
            ex.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            MediaFormat in = ex.getTrackFormat(0);
            ex.selectTrack(0);
            codec = MediaCodec.createDecoderByType(in.getString(MediaFormat.KEY_MIME));
            codec.configure(in, null, null, 0);
            codec.start();
            return drain(ex, codec, in);
        } catch (RuntimeException e) {
            throw new IOException("Không giải mã được âm thanh", e);
        } finally {
            if (codec != null) { try { codec.stop(); } catch (RuntimeException ignored) { } codec.release(); }
            ex.release();
            try { fd.close(); } catch (IOException ignored) { }
        }
    }

    private Pcm drain(MediaExtractor ex, MediaCodec codec, MediaFormat in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 20);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int rate = in.getInteger(MediaFormat.KEY_SAMPLE_RATE), channels = in.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        boolean inputDone = false, outputDone = false;
        while (!outputDone) {
            boolean fed = false;
            while (!inputDone) {                       // nạp hết các buffer đầu vào đang rảnh, không chờ
                int i = codec.dequeueInputBuffer(0);
                if (i < 0) break;
                ByteBuffer b = codec.getInputBuffer(i);
                int n = ex.readSampleData(b, 0);
                if (n < 0) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true; }
                else { codec.queueInputBuffer(i, 0, n, ex.getSampleTime(), 0); ex.advance(); }
                fed = true;
            }
            // lấy hết đầu ra đang có; chỉ chờ khi không nạp thêm được gì
            long wait = fed ? 0 : TIMEOUT_US;
            for (int o; (o = codec.dequeueOutputBuffer(info, wait)) != MediaCodec.INFO_TRY_AGAIN_LATER && !outputDone; wait = 0) {
                if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat f = codec.getOutputFormat();
                    rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                } else if (o >= 0) {
                    ByteBuffer b = codec.getOutputBuffer(o);
                    b.position(info.offset).limit(info.offset + info.size);
                    byte[] chunk = new byte[info.size];
                    b.get(chunk);
                    append(out, chunk, channels);
                    codec.releaseOutputBuffer(o, false);
                    outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                }
            }
        }
        if (out.size() == 0) throw new IOException("Không có dữ liệu PCM");
        return new Pcm(out.toByteArray(), rate);
    }

    /** Ghi PCM vào bộ đệm; nếu nhiều kênh thì chỉ lấy kênh trái (nhạc nền là mono). */
    private static void append(ByteArrayOutputStream out, byte[] chunk, int channels) {
        if (channels <= 1) { out.write(chunk, 0, chunk.length); return; }
        int frame = 2 * channels;
        for (int p = 0; p + frame <= chunk.length; p += frame) out.write(chunk, p, 2);
    }
}
