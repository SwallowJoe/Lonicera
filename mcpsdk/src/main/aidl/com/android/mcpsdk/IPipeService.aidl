// IPipeService.aidl
package com.android.mcpsdk;


interface IPipeService {
    ParcelFileDescriptor openPipe(String name);
}