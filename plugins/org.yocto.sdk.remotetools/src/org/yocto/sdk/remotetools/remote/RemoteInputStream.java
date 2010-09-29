/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Intel              - changed for TCF processes service
 *******************************************************************************/
package org.yocto.sdk.remotetools.remote;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.tm.tcf.protocol.IToken;
import org.eclipse.tm.tcf.services.IStreams;
import org.eclipse.tm.tcf.util.TCFTask;

public class RemoteInputStream extends InputStream /* implements IRemoteStream*/ {
	
	private static class Buffer {

        final long offset;

        IToken token;
        byte[] buf;
        boolean eos;

        Buffer(long offset) {
            this.offset = offset;
        }

        public String toString() {
            return "[" + offset + ":" + (buf == null ? "null" : Integer.toString(buf.length)) + "]";
        }
    }
	
	private IStreams stream;
	private String id;
	private final int buf_size;
	private long offset = 0;
    private Buffer buf;
    
	private boolean connected=true;
	
	RemoteInputStream(IStreams stream, String id, int buf_size) {
		this.stream=stream;
		this.id=id;
		this.buf_size=buf_size;
		//RemoteStreamManager.add(id, this);
	}
	
	RemoteInputStream(IStreams stream, String id) {
		this(stream,id,0x1000);
	}

	@Override
    public synchronized int read() throws IOException {
        if (!connected) throw new IOException("Stream is closed");
        while (buf == null || buf.offset > offset || buf.offset + buf.buf.length <= offset) {
            if (buf != null && buf.eos) return -1;
            buf = new TCFTask<Buffer>() {
                public void run() {
                    stream.read(id, buf_size, new IStreams.DoneRead() {
                        public void doneRead(IToken token, Exception error, 
                        		int lost_size, byte[] data, boolean eos) {
                            if (error != null) {
                                error(error);
                                return;
                            }
                            //assert data != null && data.length <= buf_size;
                            Buffer buf = new Buffer(offset);
                            if(data!=null) {
                            	assert data.length <= buf_size;
                            	buf.buf = data;
                            }
                            else {
                            	buf.buf = new byte[0];
                            }
                            buf.eos = eos;
                            done(buf);
                        }
                    });
                }
            }.getIO();
            assert buf.token == null;
        }
        int ofs = (int)(offset++ - buf.offset);
        return buf.buf[ofs] & 0xff;
    }

    @Override
    public synchronized int read(final byte arr[], final int off, final int len) throws IOException {
        if (!connected) throw new IOException("Stream is closed");
        if (arr == null) throw new NullPointerException();
        if (off < 0 || len < 0 || len > arr.length - off) throw new IndexOutOfBoundsException();
        int pos = 0;
        
        while (pos < len) {
            if (buf != null && buf.offset <= offset && buf.offset + buf.buf.length > offset) {
                int buf_pos = (int)(offset - buf.offset);
                int buf_len = buf.buf.length - buf_pos;
                int n = len - pos < buf_len ? len - pos : buf_len;
                System.arraycopy(buf.buf, buf_pos, arr, off + pos, n);
                pos += n;
                offset += n;
            }
            else if(pos == 0){
            	//got nothing yet 
                int c = read();
                if (c == -1) {
                    if (pos == 0) return -1;
                    break;
                }
                arr[off + pos++] = (byte)c;
            }else {
            	//already got something
            	break;
            }
        }
        return pos;
    }
    
    public synchronized void close() throws IOException {
        if (!connected) return;
        new TCFTask<Object>() {
            public void run() {
                stream.disconnect(id, new IStreams.DoneDisconnect() {
                    public void doneDisconnect(IToken token, Exception error) {
                    	if (error != null) error(error);
                        else  {
                        	connected=false;
                        	done(this);
                        }
                    }
                });
            }
        }.getIO();
        connected=false;
        buf=null;
    }

}
