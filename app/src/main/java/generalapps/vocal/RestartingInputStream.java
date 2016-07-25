package generalapps.vocal;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

/**
 * Created by edeetee on 20/07/2016.
 */
public class RestartingInputStream implements TarsosDSPAudioInputStream {
    File mFile;
    InputStream mBuffer;
    TarsosDSPAudioFormat mFormat;
    Lock bufferLock;

    public RestartingInputStream(File file, TarsosDSPAudioFormat format){
        mFormat = format;
        mFile = file;
        bufferLock = new ReentrantLock();
        reset();
    }

    @Override
    public long skip(long bytesToSkip) {
        return 0;
    }

    public void stop(){
        bufferLock.lock();
        mBuffer = null;
        bufferLock.unlock();
    }

    public void reset(){
        bufferLock.lock();
        if(mBuffer != null)
            try{
                mBuffer.close();
            } catch(IOException e){
                Log.e("RestartingInputStream", "Buffer close failed", e);
            }
        try{
            mBuffer = new BufferedInputStream(new FileInputStream(mFile));
        } catch(IOException e){
            Log.e("RestartingInputStream", "Buffer creation failed", e);
        }
        bufferLock.unlock();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(mBuffer != null && bufferLock.tryLock())
            try{
                return mBuffer.read(b, off, len);
            } finally {
                bufferLock.unlock();
            }
        else
            return 0;
    }

    @Override
    public void close() {
        bufferLock.lock();
        mBuffer = null;
        bufferLock.unlock();
    }

    @Override
    public TarsosDSPAudioFormat getFormat() {
        return mFormat;
    }

    @Override
    public long getFrameLength() {
        return -1;
    }
}
