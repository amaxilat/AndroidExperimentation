package org.ambientdynamix.core;

public interface IDynamixFrameworkListener {
    void onDynamixInitializing();

    void onDynamixInitializingError(final String message);

    void onDynamixInitialized(final DynamixService dynamix);

    void onDynamixStarting();

    void onDynamixStarted();

    void onDynamixStopping();

    void onDynamixStopped();

    void onDynamixError(final String message);
}
