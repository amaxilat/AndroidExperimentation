package eu.smartsantander.androidExperimentation.operations;

import java.lang.reflect.Type;
import java.util.ArrayList;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import eu.smartsantander.androidExperimentation.jsonEntities.Reading;
import eu.smartsantander.androidExperimentation.jsonEntities.Report;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class DynamixServiceListenerUtility {

    public static IDynamixListener getListener() {
        return new IDynamixListener.Stub() {
            private final String TAG = "DSListenerUtility";

            @Override
            public void onDynamixListenerAdded(String listenerId)
                    throws RemoteException {

            }

            @Override
            public void onDynamixListenerRemoved() throws RemoteException {
            }

            @Override
            public void onSessionOpened(String sessionId) throws RemoteException {
                final Result r = DynamixService.dynamix.addContextSupport(DynamixService.dynamixCallback, "org.ambientdynamix.contextplugins.ExperimentPlugin");
                for (final ContextPluginInformation pluginInformation : DynamixService.dynamix.getAllContextPluginInformation().getContextPluginInformation()) {
                    if (pluginInformation.isEnabled()) {
                        DynamixService.dynamix.addContextSupport(DynamixService.dynamixCallback, pluginInformation.getPluginId());
                    }
                }
                DynamixService.sessionStarted = true;
                Log.d(TAG, "SESSION STATUS" + r.getMessage());
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

                NotificationHQManager noteManager = NotificationHQManager.getInstance();
                Log.d(TAG, "Event timestamp " + event.getTimeStamp().toString());
                // Log each string-based context type format supported by the
                // event
                for (final String format : event.getStringRepresentationFormats()) {
                    Log.d(TAG, "Event string-based format: " + format + " size: "
                            + event.getStringRepresentation(format).length());
                }
                // Check for native IContextInfo
                if (event.hasIContextInfo()) {

                    Log.d(TAG, "Event contains native IContextInfo: " + event.getIContextInfo());
                    final IContextInfo nativeInfo = event.getIContextInfo();
                    final String msg = nativeInfo.getStringRepresentation("");
                    try {
                        Log.d(TAG, "Received Experiment/Plugin Info: " + msg);
                        final PluginInfo plugInfo = (new Gson()).fromJson(msg, PluginInfo.class);
                        if (plugInfo != null && plugInfo.getContext() != null && plugInfo.getContext().equals("org.ambientdynamix.contextplugins.ExperimentPlugin")) {
                            final String readingMsg = plugInfo.getPayload();
                            final Type listType = new TypeToken<ArrayList<Reading>>() {
                            }.getType();
                            final List<Reading> readings = (new Gson()).fromJson(readingMsg, listType);
                            final Report rObject = new Report(DynamixService.getExperiment().getId().toString());
                            rObject.setDeviceId(DynamixService.getPhoneProfiler().getPhoneId());
                            noteManager.postNotification(readingMsg);
                            final List<String> mlist = new ArrayList<>();
                            for (final Reading reading : readings) {
                                Log.d(TAG, "Received Reading: " + reading);
                                DynamixService.cacheExperimentalMessage(readingMsg);
                                if (DynamixService.getExperiment() == null) {
                                    return;
                                }
                                mlist.add(reading.getValue());
                            }
                            rObject.setResults(mlist);
                            final String message = rObject.toJson();
                            Log.d(TAG, "ResultMessage:message " + message);
                            DynamixService.publishMessage(message);
                        } else {
                            final String readingMsg = plugInfo.getPayload();
                            final Type listType = new TypeToken<ArrayList<Reading>>() {
                            }.getType();
                            final List<Reading> readings = (new Gson()).fromJson(readingMsg, listType);
                            for (final Reading reading : readings) {
                                //Toast.makeText(DynamixService.getAndroidContext(),reading.getContext(), 500).show();
                                String notification = reading.getContext();
                                if (notification.contains("Gps")) {
                                    notification += ":" + reading.getValue();
                                }
                                noteManager.postNotification(notification);
                                DynamixService.getReadingStorage().pushReading(reading);
                                DynamixService.getPhoneProfiler().incMsgCounter();
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Bad Formed Reading" + msg);
                    }
                }
            }

            @Override
            public void onContextSupportAdded(ContextSupportInfo info) throws RemoteException {
                Log.d(TAG, "CONTENT SUPPORT ADDED: "
                        + info.getPlugin().getPluginId());
                DynamixService.sessionStarted = true;
            }

            @Override
            public void onContextSupportRemoved(ContextSupportInfo info) throws RemoteException {
                // TODO Auto-generated method stub
            }

            @Override
            public void onContextTypeNotSupported(String contextType) throws RemoteException {
                Log.d(TAG, "CONTENT NO SUPPORTED" + contextType);
                DynamixService.sessionStarted = false;
            }

            @Override
            public void onInstallingContextSupport(final ContextPluginInformation plugin,
                                                   String contextType) throws RemoteException {
                // TODO Auto-generated method stub
            }

            @Override
            public void onInstallingContextPlugin(ContextPluginInformation plugin)
                    throws RemoteException {
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
            public void onContextPluginInstallFailed(ContextPluginInformation plug, String message)
                    throws RemoteException {
                // TODO Auto-generated method stub
            }

            @Override
            public void onContextRequestFailed(String requestId, String message, int errorCode)
                    throws RemoteException {
                // TODO Auto-generated method stub
            }

            @Override
            public void onContextPluginDiscoveryStarted() throws RemoteException {
                // TODO Auto-generated method stub
            }

            @Override
            public void onContextPluginDiscoveryFinished(
                    final List<ContextPluginInformation> discoveredPlugins) throws RemoteException {
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
                Log.d(TAG, "Plugin Error " + plug.getPluginDescription() + "," + message);
            }
        };

    }

    public static ServiceConnection createServiceConnection() {
        return new ServiceConnection() {
            private final String TAG = this.getClass().getSimpleName();

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    DynamixService.dynamix = IDynamixFacade.Stub.asInterface(service);
                    DynamixService.dynamix.addDynamixListener(DynamixService.dynamixCallback);
                    DynamixService.dynamix.openSession();
                    Log.d(TAG, "Experiment Connected");
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
        DynamixService.dynamixCallback = DynamixServiceListenerUtility.getListener();
        DynamixService.sConnection = DynamixServiceListenerUtility.createServiceConnection();
        DynamixService.getAndroidContext().bindService(new Intent(DynamixService.getAndroidContext(), DynamixService.class), DynamixService.sConnection, Context.BIND_AUTO_CREATE);
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
