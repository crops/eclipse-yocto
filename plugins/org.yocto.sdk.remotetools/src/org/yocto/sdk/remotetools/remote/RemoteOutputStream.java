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
import java.io.OutputStream;

import org.eclipse.tm.tcf.protocol.IToken;
import org.eclipse.tm.tcf.services.IStreams;
import org.eclipse.tm.tcf.util.TCFTask;

public class RemoteOutputStream extends OutputStream /* implements IRemoteStream */{

	private IStreams stream;
	private String id;
	//private boolean eos=false;
	private boolean connected=true;
	
	RemoteOutputStream(IStreams stream, String id) {
		this.stream=stream;
		this.id=id;
		//RemoteStreamManager.add(id, this);
	}
	
	@Override
    public synchronized void write(int b) throws IOException {
        final byte[] buf = new byte[1];
        buf[0] = (byte)b;
        this.write(buf, 0, 1);
    }
	
	@Override
    public synchronized void write(final byte b[], final int off, final int len) throws IOException {
        if (!connected) new IOException("Stream is closed");
        System.out.println("RemoteOutputStream write: " + new String(b, off, len) );
    	new TCFTask<Object>() {
            public void run() {
                stream.write(id, b, off, len, new IStreams.DoneWrite() {
                    public void doneWrite(IToken token, Exception error) {
                        // TODO: stream write error handling
                        if (error != null) error(error);
                        done(this);
                    }
                });
            }
        }.getIO();
    }
	
	public synchronized void close() throws IOException {
        if (!connected) return;
        new TCFTask<Object>() {
            public void run() {
                stream.disconnect(id, new IStreams.DoneDisconnect() {
                    public void doneDisconnect(IToken token, Exception error) {
                    	if (error != null) error(error);
                        else {
                        	connected=false;
                        	done(this);
                        }
                    }
                });
            }
        }.getIO();
        connected=false;
    }
}
