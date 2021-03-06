package tech.qt.com.meishivideoeditsdk.camera.filter;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import tech.qt.com.meishivideoeditsdk.camera.OpenGLUtils;

/**
 * Created by chenchao on 2017/12/8.
 */

public class GPUGourpFilter extends GPUFilter {
    public List<GPUFilter>mfilters;
//    private boolean isInitlized;

    public void addFilter(GPUFilter gpuFilter){
        mfilters.add(gpuFilter);
    }
    public int getFilterCount(){
        return mfilters.size();
    }

    public void removeAllFilter(){
        synchronized (mfilters) {
            mfilters.clear();
        }
    }
    public GPUGourpFilter(){
        mfilters = new ArrayList<>();
        mRunOnDraw = new LinkedList<Runnable>();
    }
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;
    @Override
    public void setNeedRealse(boolean needRealse) {
        this.needRealse = needRealse;
    }

    @Override
    public void init(){

        if(mfilters!=null){
            int size = mfilters.size();
            for(int i = 0 ;i< size ;i ++){
                GPUFilter filter = mfilters.get(i);
                filter.init();
            }
        }
    }
    public void filtersChanged(final int width, final int height){

        runOnDraw(new Runnable() {
            @Override
            public void run() {
                release();
                int size = mfilters.size();

                mFrameBuffers = new int[size - 1];
                mFrameBufferTextures = new int[size - 1];

                for (int i = 0; i < size - 1; i++) {

//                    if(i<size -2) {

                        GLES20.glGenTextures(1, mFrameBufferTextures, i);
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i]);
                        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                        GLES20.glGenFramebuffers(1, mFrameBuffers, i);
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i], 0);
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                }
            }
        });

    }

    public void onDrawFrame(int textureId, SurfaceTexture st, int mViewWidth, int mViewHeight){
        runPendingOnDrawTasks();
//        if(isInitlized == false) return;
        if (mfilters != null) {

            OpenGLUtils.checkGlError("group1");
            synchronized (mfilters) {
                int size = mfilters.size();
                int previousTexture = textureId;
                for (int i = 0; i < size; i++) {
                    GPUFilter filter = mfilters.get(i);
                    boolean isNotLast = i < size - 1;
                    if (isNotLast) {//除最后一个Filter 外在自己创建FrameBuffer开始渲染，最后一个Filter在系统默认的Filter 渲染

//                    GLES11Ext.glBindFramebufferOES(GLES11Ext.GL_FRAMEBUFFER_OES, mFrameBuffers[i]);
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                        GLES20.glClearColor(0, 0, 0, 0);
//                    GLES11Ext.glClearColorxOES(0,0,0,0);
                        OpenGLUtils.checkGlError("group2");
                    }

                    filter.onDrawFrame(previousTexture, st, mViewWidth, mViewHeight);

                    if (isNotLast) {
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//                    GLES11Ext.glBindFramebufferOES(GLES11Ext.GL_FRAMEBUFFER_OES, 0);
                        previousTexture = mFrameBufferTextures[i];
                    }
                }
            }
        }
    }
    @Override
    public void release(){

                if(mFrameBuffers!=null&&mFrameBuffers.length>0) {
                    GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
                    mFrameBuffers = null;
                }

                if(mFrameBufferTextures!=null&&mFrameBufferTextures.length>0)
                {
                    GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
                }

    }

}
