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
import org.ambientdynamix.util.Log;

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
            public void onSessionOpened(String sessionId) throws RemoteException {
                final Result r = DynamixService.dynamix.addContextSupport(DynamixService.dynamixCallback, "org.ambientdynamix.contextplugins.ExperimentPlugin");
                for (final ContextPluginInformation pluginInformation : DynamixService.dynamix.getAllContextPluginInformation().getContextPluginInformation()) {
                    if (pluginInformation.isEnabled()) {
                        DynamixService.dynamix.addContextSupport(DynamixService.dynamixCallback, pluginInformation.getPluginId());
                    }
                }
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

                NotificationHQManager noteManager = NotificationHQManager.getInstance();

                Log.w(TAG, "Event timestamp "
                        + event.getTimeStamp().toString());
                if (event.expires())
                    Log.w(TAG, "Event expires at "
                            + event.getExpireTime().toString());
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

                    Log.w(TAG, "Event contains native IContextInfo: " + event.getIContextInfo());
                    IContextInfo nativeInfo = event.getIContextInfo();
                    String msg = nativeInfo.getStringRepresentation("");
                    try {
                        Log.w(TAG, "Received Experiment/Plugin Info: " + msg);
                        PluginInfo plugInfo = (new Gson()).fromJson(msg, PluginInfo.class);
                        if (plugInfo != null && plugInfo.getContext() != null && plugInfo.getContext().equals("org.ambientdynamix.contextplugins.ExperimentPlugin")) {
                            String readingMsg = plugInfo.getPayload();
                            Type listType = new TypeToken<ArrayList<Reading>>() {
                            }.getType();
                            final List<Reading> readings = (new Gson()).fromJson(readingMsg, listType);
                            final Report rObject = new Report(DynamixService.getExperiment().getId().toString());
                            rObject.setDeviceId(DynamixService.getPhoneProfiler().getPhoneId());
                            final List<String> mlist = new ArrayList<>();
                            for (final Reading reading : readings) {
                                Log.w(TAG, "Received Reading: " + reading);
                                noteManager.postNotification(readingMsg);
                                DynamixService.cacheExperimentalMessage(readingMsg);
                                if (DynamixService.getExperiment() == null)
                                    return;
                                mlist.add(reading.getValue());
                            }
                            rObject.setResults(mlist);
                            final String message = rObject.toJson();
                            Log.i(TAG, "ResultMessage:message " + message);
                            new AsyncReportNowTask().execute(message);

                        } else {
                            String readingMsg = plugInfo.getPayload();
                            Type listType = new TypeToken<ArrayList<Reading>>() {
                            }.getType();
                            List<Reading> readings = (new Gson()).fromJson(readingMsg, listType);
                            for (Reading reading : readings) {
                                //Toast.makeText(DynamixService.getAndroidContext(),reading.getContext(), 500).show();
                                String notification = reading.getContext();
                                if (notification.contains("Gps")) {
                                    notification += ":" + reading.getValue();
                                }
                                noteManager.postNotification(notification);
                                Log.w(TAG, "Plugin Reading: " + reading);
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
                    DynamixService.dynamix = IDynamixFacade.Stub.asInterface(service);
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
