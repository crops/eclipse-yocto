package org.yocto.sdk.remotetools;

import java.io.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.yocto.sdk.remotetools.remote.*;
import org.eclipse.rse.core.model.IHost;

public class InternalTest extends Job {
	
	private IHost connection;
	private RemoteTarget target;
	private String local_conn=new String("test-local");
	private String qemu_conn=new String("test-qemu");
	
	public InternalTest() {
		super("LLU InternalTest Job");
	}

	private void prepare(String conn, IProgressMonitor monitor) throws Exception{
		connection=RSEHelper.getRemoteConnectionByName(conn);
		if(connection==null)
			throw new Exception("Not found RSE connection "+conn);
		
		target=new RemoteTarget(conn);
		
		target.connect(monitor);
		return;
	}
	
	private void finish (String conn, IProgressMonitor monitor) throws Exception{
		target.disconnect(monitor);
		return;
	}
	
	private void print_result(String name, int ret) {
		if(ret!=0) {
			System.out.println("[FAIL]"+name);
		}else {
			System.out.println("[OK]"+name);
		}
			
	}
	
	private int test_file(IProgressMonitor monitor) {
		try {
			RSEHelper.putRemoteFile(connection, "/tmp/localTestFile1", "/tmp/remoteTestFile1", monitor);
		}catch (Exception e){
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	private int test_exec(IProgressMonitor monitor, boolean long_run) {
		String exec;
		String arg[]=new String [1];
		
		if(long_run) {
			exec=new String("/home/lulianhao/test/long");
		}else {
			exec=new String("/home/lulianhao/test/hello");
		}
		arg[0]=exec;
		
		try {
			RemoteApplication app=new RemoteApplication(target,
				null,
				exec);
			
			app.start(arg,null);
			
			InputStream in=app.getInputStream();
			OutputStream out=app.getOutputStream();
			InputStream err=app.getErrStream();
			byte buf[]=new byte [100];
			if(!long_run) {
				while(in.read(buf)!=-1) {
					System.out.printf("%s",new String(buf));
				}
			}else {
				int i=0;
				for(i=0;i<3 && in.read(buf)!=-1;i++)  
					System.out.printf("%s",new String(buf));
			}
			in.close();
			out.close();
			if(err!=null)
				err.close();
			
			app.terminate();

			System.out.printf("%s exit with %d\n",exec,app.waitFor(null));
			
			
		}catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	private void testAll(String conn, IProgressMonitor monitor) {

		try {
			prepare(conn,monitor);
		}catch (Exception e){
			e.printStackTrace();
			print_result("prepare "+conn, -1);
			return;
		}
		
		print_result("test_file "+conn,test_file(monitor));
		print_result("test_exec "+conn,test_exec(monitor,false));
		print_result("test_exec(long_run) "+conn,test_exec(monitor,true));
		
		try {
			finish(conn,monitor);
		}catch (Exception e){
			e.printStackTrace();
			print_result("finish "+conn, -1);
			return;
		}
	}
	
	private void testlocal() {
		String[] cmdarray=new String[1];
		cmdarray[0]=new String("/home/lulianhao/test/hello");
		Process proc;
		try {
			proc=Runtime.getRuntime().exec(cmdarray,null,null);
		}catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		InputStream in=proc.getInputStream();
		try {
			int i;
			while((i=in.read())!=-1) {
				System.out.printf("%c",(char)i);
			}
		}catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
	}
	@Override
	public IStatus run(IProgressMonitor monitor) {
		//testlocal();
		testAll(local_conn,monitor);
		//testAll(qemu_conn,monitor);
		return Status.OK_STATUS;
	}

}
