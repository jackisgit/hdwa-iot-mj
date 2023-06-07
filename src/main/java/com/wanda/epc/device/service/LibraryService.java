package com.wanda.epc.device.service;
public interface LibraryService {

    /**
     * init 
     * Create tmp dir, add to java library path.
     */
    void init();
    void copyToJavaLibraryPath();
    void loadLibrary(String dllName);
}
