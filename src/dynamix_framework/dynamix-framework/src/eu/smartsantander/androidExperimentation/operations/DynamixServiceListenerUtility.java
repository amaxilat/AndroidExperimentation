package eu.smartsantander.androidExperimentation.operations;

import java.util.List;

import org.ambientdynamix.api.application.ContextEvent;
import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.ContextSupportInfo;
import org.ambientdynamix.api.application.IContextInfo;
import org.ambientdynamix.api.application.IDynamixFacade;
import org.ambientdynamix.api.application.IDynamixListener;
import org.ambientdynamix.api.application.Result;
import org.ambientdynamix.contextplugins.ExperimentPlugin.PluginInfo;
import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.util.Log;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.jsonEntities.Reading;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

public class DynamixServiceListenerUtility {

	public static IDynamixListener getListerner() {
		return new IDynamixListener.Stub() {
			private final String TAG = this.getClass().getSimpleName();

			@Override
			public void onDynamixListenerAdded(String listenerId)
					throws RemoteException {

			}

			@Override
			public void onDynamixListenerRemoved() throws RemoteException {
			}

			@Override
			public void onSessionOpened(String sessionId)	throws RemoteException {
				Result r = DynamixService.dynamix.addContextSupport(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.ExperimentPlugin");
				r = DynamixService.dynamix.addContextSupport(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.GpsPlugin");
				DynamixService.sessionStarted = true;
				Log.w(TAG, "SESSION STATUS" + r.getMessage());
			}

			@Override
			public void onSessionClosed() throws RemoteException {
				DynamixService.sessionStarted = false;
			}

			@Override
			public void onAwaitingSecurityAuthorization()
					throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSecurityAuthorizationGranted() throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSecurityAuthorizationRevoked() throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextEvent(ContextEvent event)
					throws RemoteException {
				Log.w(TAG, "Event timestamp "
						+ event.getTimeStamp().toLocaleString());
				if (event.expires())
					Log.w(TAG, "Event expires at "
							+ event.getExpireTime().toLocaleString());
				else
					Log.i(TAG, "Event does not expire");
				// Log each string-based context type format supported by the
				// event
				for (String format : event.getStringRepresentationFormats()) {
					Log.w(TAG, "Event string-based format: " + format
							+ " size: "
							+ event.getStringRepresentation(format).length());
					Log.w(TAG,
							"Event string-based format: " + format
									+ " contained: "
									+ event.getStringRepresentation(format));
				}
				// Check for native IContextInfo
				if (event.hasIContextInfo()) {
					
					Log.w(TAG,"Event contains native IContextInfo: "+ event.getIContextInfo());
					IContextInfo nativeInfo = event.getIContextInfo();
					String msg = nativeInfo.getStringRepresentation("");
					try{
						Log.w(TAG, "Received Experiment/Plugin Info: " + msg);
						PluginInfo plugInfo=(new Gson()).fromJson(msg, PluginInfo.class);
						String readingMsg=plugInfo.getPayload();
						Reading reading=Reading.fromJson(readingMsg);
						Log.w(TAG, "Received Reading: " + reading);
						Toast.makeText(DynamixService.getAndroidContext(), readingMsg,	8000).show();
						if(reading.getContext().equals("org.ambientdynamix.contextplugins.ExperimentPlugin")){
							// push back on experimentation reading 
							
						}else{
							DynamixService.getReadingStorage().pushReading(reading);						
						}
					}catch(Exception e){
						Log.w(TAG, "Bad Formed Reading" + msg);
					}
				}
			}

			@Override
			public void onContextSupportAdded(ContextSupportInfo supportInfo)
					throws RemoteException {
				Log.w(TAG, "CONTENT SUPPORT ADDED: "
						+ supportInfo.getPlugin().getPluginId());
				DynamixService.sessionStarted = true;
			}

			@Override
			public void onContextSupportRemoved(ContextSupportInfo supportInfo)
					throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextTypeNotSupported(String contextType)
					throws RemoteException {
				Log.w(TAG, "CONTENT NO SUPPORTED" + contextType);
				DynamixService.sessionStarted = false;
			}

			@Override
			public void onInstallingContextSupport(
					ContextPluginInformation plugin, String contextType)
					throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onInstallingContextPlugin(
					ContextPluginInformation plugin) throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginInstallProgress(
					ContextPluginInformation plugin, int percentComplete)
					throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginInstalled(ContextPluginInformation plugin)
					throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginUninstalled(
					ContextPluginInformation plugin) throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginInstallFailed(
					ContextPluginInformation plug, String message)
					throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextRequestFailed(String requestId,
					String message, int errorCode) throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginDiscoveryStarted()
					throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onContextPluginDiscoveryFinished(
					List<ContextPluginInformation> discoveredPlugins)
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
			public void onContextPluginError(ContextPluginInformation plug,
					String message) throws RemoteException {
				Log.i(TAG, "Pluginf Error " + plug.getPluginDescription() + ","
						+ message);

			}
		};

	}

	public static ServiceConnection createServiceConnection() {
		return new ServiceConnection() {
			private final String TAG = this.getClass().getSimpleName();

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				try {
					DynamixService.dynamix = IDynamixFacade.Stub	.asInterface(service);
					DynamixService.dynamix.addDynamixListener(DynamixService.dynamixCallback);
					DynamixService.dynamix.openSession();
					Log.w(TAG, "Experiment Connected");
				} catch (Exception e) {
					Log.w(TAG, e.getMessage());
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				DynamixService.dynamix = null;
			}
		};
	}

	public static void BindService() {
		DynamixService.getAndroidContext().bindService(
				new Intent(IDynamixFacade.class.getName()),
				DynamixService.sConnection, Context.BIND_AUTO_CREATE);
	}

	public static void start() {
		DynamixService.dynamixCallback = DynamixServiceListenerUtility.getListerner();
		DynamixService.sConnection = DynamixServiceListenerUtility.createServiceConnection();
		DynamixService.getAndroidContext().bindService(	new Intent(IDynamixFacade.class.getName()),DynamixService.sConnection, Context.BIND_AUTO_CREATE);
	}

	public static void stop() {
		try {
			DynamixService.dynamix.removeAllContextSupport();
			DynamixService.dynamix.closeSession();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
