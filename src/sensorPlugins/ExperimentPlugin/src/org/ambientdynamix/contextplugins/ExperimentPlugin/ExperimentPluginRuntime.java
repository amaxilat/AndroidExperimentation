package org.ambientdynamix.contextplugins.ExperimentPlugin;

import java.util.List;
import java.util.UUID;

import org.ambientdynamix.api.application.ContextEvent;
import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.ContextSupportInfo;
import org.ambientdynamix.api.application.IContextInfo;
import org.ambientdynamix.api.application.IDynamixFacade;
import org.ambientdynamix.api.application.IDynamixListener;
import org.ambientdynamix.api.application.Result;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.ReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ExperimentPluginRuntime extends ReactiveContextPluginRuntime {
	
    private final String TAG = this.getClass().getSimpleName();
	private Context context;
	private String msg;
	private String gpsPayload="";
	private IDynamixFacade dynamix;	
	private IDynamixListener dynamixCallback;	
	private ServiceConnection sConnection;
		
	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		this.msg="";
		
		
		dynamixCallback = new IDynamixListener.Stub() {
	        private boolean sessionStarted;

			@Override
	        public void onDynamixListenerAdded(String listenerId) throws RemoteException {
	        }
	 
	        @Override
	        public void onDynamixListenerRemoved() throws RemoteException {
	        }
	 
	        @Override
			public void onSessionOpened(String sessionId)throws RemoteException {
				Result r=dynamix.addContextSupport(dynamixCallback, "org.ambientdynamix.contextplugins.GpsPlugin");
				this.sessionStarted=true;
				Log.w(TAG,	"SESSION STATUS"	+r.getMessage());
			}

			@Override
			public void onSessionClosed() throws RemoteException {
				this.sessionStarted=false;
			}

			@Override
			public void onAwaitingSecurityAuthorization() throws RemoteException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onContextEvent(ContextEvent event) throws RemoteException {
				Log.w(TAG, "Event timestamp "+ event.getTimeStamp().toLocaleString());
				if (event.expires())
					Log.w(TAG, "Event expires at "+ event.getExpireTime().toLocaleString());
				else
					Log.w(TAG, "Event does not expire");
				// Log each string-based context type format supported by the
				// event
				for (String format : event.getStringRepresentationFormats()) {
					Log.w(TAG, "Event string-based format: " + format+ " size: "+ event.getStringRepresentation(format).length());
					Log.w(TAG,"Event string-based format: " + format+ " contained: "+ event.getStringRepresentation(format));
				}
				// Check for native IContextInfo
				if (event.hasIContextInfo()) {
					Log.w(TAG,"Event contains native IContextInfo: "+ event.getIContextInfo());
					IContextInfo nativeInfo = event.getIContextInfo();
					IPluginInfo info = (IPluginInfo) nativeInfo;
					Log.w(TAG,"Received ExperimentInfo: "+ info.getPayload());
					gpsPayload=info.getPayload();				
				}
				
			}

			@Override
			public void onContextSupportAdded(ContextSupportInfo supportInfo)throws RemoteException {				
					Log.w(TAG,	"CONTENT SUPPORT ADDED: "	+ supportInfo.getPlugin().getPluginId());	
					 this.sessionStarted=true;
			}

			@Override
			public void onContextSupportRemoved(ContextSupportInfo supportInfo)	throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextTypeNotSupported(String contextType)throws RemoteException {
				Log.w(TAG,	"CONTENT NO SUPPORTED"	+ contextType);
				this.sessionStarted=false;
			}

			@Override
			public void onInstallingContextSupport(			ContextPluginInformation plugin, String contextType)throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onInstallingContextPlugin(ContextPluginInformation plugin) throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginInstallProgress(	ContextPluginInformation plugin, int percentComplete)	throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginInstalled(ContextPluginInformation plugin)throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginUninstalled(ContextPluginInformation plugin) throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginInstallFailed(ContextPluginInformation plug, String message)throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextRequestFailed(String requestId,String message, int errorCode) throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginDiscoveryStarted()throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginDiscoveryFinished(List<ContextPluginInformation> discoveredPlugins)
					throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onDynamixFrameworkActive() throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onDynamixFrameworkInactive() throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginError(ContextPluginInformation plug,	String message) throws RemoteException {
				Log.w(TAG,	"Pluginf Error "	+plug.getPluginDescription() +","+message);

			}

			@Override
			public void onSecurityAuthorizationGranted() throws RemoteException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSecurityAuthorizationRevoked() throws RemoteException {
				// TODO Auto-generated method stub
				
			}
		};
		
		sConnection = new ServiceConnection() {
	        @Override
	        public void onServiceConnected(ComponentName name, IBinder service) {
	            try {
	                dynamix = IDynamixFacade.Stub.asInterface(service);
	                dynamix.addDynamixListener(dynamixCallback);
	                dynamix.openSession();
	            } catch (Exception e) {
	                Log.w(TAG, e);
	            }
	        }
	        
	        @Override	        
	        public void onServiceDisconnected(ComponentName name) {
	            // We've been disconnected, so null out our existing IDynamixFacade
	            dynamix = null;
	        }
		 
	 
	    };
	    
	    this.context.bindService(new Intent(IDynamixFacade.class.getName()),  sConnection, Context.BIND_AUTO_CREATE);
		Log.w(TAG, "Experiment Inited!");
		
	    
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType)
	{	
		Log.w(TAG, "Experiment handleContextRequest!");
		try {
			if (dynamix==null){
				PluginInfo info = new PluginInfo();
				info.setState("STILL INACTIVE");
				info.setPayload(this.msg);
				sendContextEvent(requestId, new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);	
				this.context.bindService(new Intent(IDynamixFacade.class.getName()),  sConnection, Context.BIND_AUTO_CREATE);
				Log.w(TAG, "dymaix still null");
				return;
			}
			Result r=dynamix.contextRequest(dynamixCallback,"org.ambientdynamix.contextplugins.GpsPlugin", "org.ambientdynamix.contextplugins.GpsPlugin");						
		} catch (Exception e) {
			Log.w("Experiment Workload Error", e.toString());
			this.msg="";
		}
	        
		Log.w("Experiment Message:", this.msg);
		PluginInfo info = new PluginInfo();
		info.setState("ACTIVE");
		info.setPayload(this.msg);
		sendContextEvent(requestId, new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);	
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config){
		handleContextRequest(requestId,contextType);
	}	
	
	@Override
	public void start()
	{		
		
	}
	
	@Override
	public void stop()
	{
 	
	}

	@Override
	public void destroy() {
		/*
		 * At this point, the plug-in should stop and release any resources. Nothing to do in this case except for stop.
		 */
		this.stop();
		Log.d(TAG, "Experiment Destroyed!");
	}

	@Override
	public void updateSettings(ContextPluginSettings settings) {
		// Not supported
	}

	@Override
	public void setPowerScheme(PowerScheme scheme) {
		// Not supported
	}

	 
	
	
 
	
 
	
	 
}