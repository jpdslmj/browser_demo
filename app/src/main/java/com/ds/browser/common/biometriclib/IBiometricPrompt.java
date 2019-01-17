package com.ds.browser.common.biometriclib;

import android.os.CancellationSignal;
import android.support.annotation.NonNull;

public interface IBiometricPrompt {

    void authenticate(@NonNull CancellationSignal cancel, @NonNull BiometricPromptManager.OnBiometricIdentifyCallback callback);

}
