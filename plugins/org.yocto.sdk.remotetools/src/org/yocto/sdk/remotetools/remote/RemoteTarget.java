/*******************************************************************************
 * Copyright (c) 2010 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.remotetools.remote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tm.tcf.protocol.IChannel;
import org.eclipse.tm.tcf.protocol.IPeer;
import org.eclipse.tm.tcf.core.AbstractPeer;
import org.eclipse.tm.tcf.protocol.IService;
import org.eclipse.tm.tcf.protocol.IToken;
import org.eclipse.tm.tcf.protocol.Protocol;
import org.eclipse.tm.tcf.services.IProcesses;
import org.eclipse.tm.tcf.services.ILocator;
import org.eclipse.tm.tcf.services.IStreams;
import org.eclipse.tm.tcf.util.TCFTask;
import org.yocto.sdk.remotetools.RSEHelper;

public class RemoteTarget {
	
	public static int TCF_PORT = 1534;
	
	private IChannel channel;
	
	private String connection_name;

	private Throwable channel_error;

	private final List<Runnable> wait_list = new ArrayList<Runnable>();

	private boolean poll_timer_started;
	
	private boolean subscribed=false;
	
	private IStreams stream;
	private IStreams.StreamsListener streamListener = new IStreams.StreamsListener() {
        public void created(String stream_type, String stream_id,
                String context_id) {
        }

        public void disposed(String stream_type, String stream_id) {
        }
    };
	
	public RemoteTarget(String connection)	{
		connection_name=connection;
	}
	
	private void add_to_wait_list(Runnable cb) {
        wait_list.add(cb);
        if (poll_timer_started) return;
        Protocol.invokeLater(1000, new Runnable() {
            public void run() {
                poll_timer_started = false;
                run_wait_list();
            }
        });
        poll_timer_started = true;
    }

    private void run_wait_list() {
        if (wait_list.isEmpty()) return;
        Runnable[] r = wait_list.toArray(new Runnable[wait_list.size()]);
        wait_list.clear();
        for (int i = 0; i < r.length; i++) r[i].run();
    }

    private boolean connectTCFChannel(Exception[] res, IProgressMonitor monitor) {
        if (channel != null) {
            switch (channel.getState()) {
            case IChannel.STATE_OPEN:
            case IChannel.STATE_CLOSED:
                synchronized (res) {
                    if (channel_error instanceof Exception) res[0] = (Exception)channel_error;
                    else if (channel_error != null) res[0] = new Exception(channel_error);
                    else res[0] = null;
                    res.notify();
                    return true;
                }
            }
        }
        if(monitor!=null) {
	        if (monitor.isCanceled()) {
	            synchronized (res) {
	                res[0] = new Exception("Canceled"); //$NON-NLS-1$
	                if (channel != null) channel.terminate(res[0]);
	                res.notify();
	                return true;
	            }
	        }
        }
        if (channel == null) {
            String host = RSEHelper.getRemoteHostName(connection_name);
            if(host == null) {
            	synchronized(res) {
            		res[0] = new Exception("Invalid connection name:"+ connection_name); //$NON-NLS-1$
                    if (channel != null) channel.terminate(res[0]);
                    res.notify();
                    return true;
            	}
            }
            IPeer peer = null;
            String port_str = Integer.toString(TCF_PORT);
            ILocator locator = Protocol.getLocator();
            for (IPeer p : locator.getPeers().values()) {
                Map<String, String> attrs = p.getAttributes();
                if ("TCP".equals(attrs.get(IPeer.ATTR_TRANSPORT_NAME)) && //$NON-NLS-1$
                        host.equalsIgnoreCase(attrs.get(IPeer.ATTR_IP_HOST)) &&
                        port_str.equals(attrs.get(IPeer.ATTR_IP_PORT))) {
                    peer = p;
                    break;
                }
            }
            if (peer == null) {
                Map<String, String> attrs = new HashMap<String, String>();
                attrs.put(IPeer.ATTR_ID, "Yocto:" + host + ":" + port_str); //$NON-NLS-1$ //$NON-NLS-2$
                attrs.put(IPeer.ATTR_NAME, host);
                attrs.put(IPeer.ATTR_TRANSPORT_NAME, "TCP"); //$NON-NLS-1$
                attrs.put(IPeer.ATTR_IP_HOST, host);
                attrs.put(IPeer.ATTR_IP_PORT, port_str);
                peer = new AbstractPeer(attrs);
            }
            subscribed=false;
            channel = peer.openChannel();
            channel.addChannelListener(new IChannel.IChannelListener() {

                public void onChannelOpened() {
                    assert channel != null;
                    run_wait_list();
                }

                public void congestionLevel(int level) {
                }

                public void onChannelClosed(Throwable error) {
                    assert channel != null;
                    channel.removeChannelListener(this);
                    channel_error = error;
                    if (wait_list.isEmpty()) {
                        //TODO
                    }
                    else {
                        run_wait_list();
                    }
                    subscribed=false;
                    stream=null;
                    channel = null;
                    channel_error = null;
                }

            });
            assert channel.getState() == IChannel.STATE_OPENNING;
        }
        return false;
    }
    
    private boolean disconnectTCFChannel(Exception[] res, IProgressMonitor monitor) {
        if (channel == null || channel.getState() == IChannel.STATE_CLOSED) {
            synchronized (res) {
                res[0] = null;
                res.notify();
                return true;
            }
        }
        if(monitor!=null) {
	        if (monitor.isCanceled()) {
	            synchronized (res) {
	                res[0] = new Exception("Canceled"); //$NON-NLS-1$
	                res.notify();
	                return true;
	            }
	        }
        }
        if (channel.getState() == IChannel.STATE_OPEN) channel.close();
        return false;
    }
    
    public synchronized void connect(final IProgressMonitor monitor) throws Exception {
        assert !Protocol.isDispatchThread();
        final Exception[] res = new Exception[1];
        
        if(monitor!=null)
        	monitor.beginTask("Connecting " + connection_name, 2); //$NON-NLS-1$
        synchronized (res) {
            Protocol.invokeLater(new Runnable() {
                public void run() {
                    if (!connectTCFChannel(res, monitor)) add_to_wait_list(this);
                }
            });
            res.wait();
        }
        if(monitor!=null)
        	monitor.worked(1);
        if (res[0] != null) {
        	monitor.done();
        	throw res[0];
        }
        
        //subscribe the stream
        stream=getStreamsService();
        try {
        	subscribe();
        }catch (Exception e) {
        	res[0]=e;
      
        	final Exception[] res2 = new Exception[1];
        	synchronized (res2) {
                Protocol.invokeLater(new Runnable() {
                    public void run() {
                        if (!disconnectTCFChannel(res2, monitor)) add_to_wait_list(this);
                    }
                });
                res2.wait();
            }
        }
        if(monitor!=null)
        	monitor.done();
        if (res[0] != null) throw res[0];
    }
    
    public synchronized void disconnect(final IProgressMonitor monitor) throws Exception {
        assert !Protocol.isDispatchThread();
        final Exception[] res = new Exception[1];
        
        if(monitor!=null)
        	monitor.beginTask("Disconnecting " + connection_name, 2); //$NON-NLS-1$
        try {
        	unsubscribe();
        } catch (Exception e) {
        	
        }
        if(monitor!=null)
        	monitor.worked(1);
        synchronized (res) {
            Protocol.invokeLater(new Runnable() {
                public void run() {
                    if (!disconnectTCFChannel(res, monitor)) add_to_wait_list(this);
                }
            });
            res.wait();
        }
        if(monitor!=null)
        	monitor.done();
        if (res[0] != null) throw res[0];
    }
    
    public boolean isConnected() {
        final boolean res[] = new boolean[1];
        Protocol.invokeAndWait(new Runnable() {
            public void run() {
               res[0] = channel != null && channel.getState() == IChannel.STATE_OPEN;
            }
        });
        return res[0];
    }
    
    public <V extends IService> V getService(Class<V> service_interface) {
        if (channel == null || channel.getState() != IChannel.STATE_OPEN) throw new Error("Not connected"); //$NON-NLS-1$
        V m = channel.getRemoteService(service_interface);
        if (m == null) throw new Error("Remote peer does not support " + service_interface.getName() + " service"); //$NON-NLS-1$  //$NON-NLS-2$
        return m;
    }
    
    public IProcesses getProcessesService() {
        return getService(IProcesses.class);
    }
    
    public IStreams getStreamsService() {
        return getService(IStreams.class);
    }  
    
    private void subscribe() throws Exception {
		new TCFTask<Object>() {
            public void run() {
                if (subscribed) {
                    done(this);   
                }
                else {
                	subscribed = true;
                    stream.subscribe(IProcesses.NAME, streamListener,
                            new IStreams.DoneSubscribe() {
                        public void doneSubscribe(IToken token,
                                Exception error) {
                            if (error != null) {
                                subscribed = false;
                                error(error);
                            }
                            else
                                done(this);
                        }

                    });
                }}
        }.get();
    }
    
    private void unsubscribe() throws Exception {
		new TCFTask<Object>() {
            public void run() {
                if (!subscribed) {
                    done(this);   
                }
                else {
                    stream.unsubscribe(IProcesses.NAME, streamListener,
                            new IStreams.DoneUnsubscribe() {
                        public void doneUnsubscribe(IToken token,
                                Exception error) {
                            if (error != null) {
                                subscribed = false;
                                error(error);
                            }
                            else
                                done(this);
                        }
                    });
                }}
        }.get();
    }	
    
    public void resubscribe() throws Exception {
    	
    }
    
    public String getRemoteHostName() {
    	return (channel==null) ? null :channel.getRemotePeer().getAttributes().get(IPeer.ATTR_IP_HOST);
    }
}
