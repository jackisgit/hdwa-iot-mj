package com.wanda.epc.device.service;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;

/**
 * @author <a href:"mailto:oujunxiao.ou@zkteco.com">oujunxiao</a>
 * @version v1.0
 */
public class LibrarayServiceImpl implements LibraryService {

    private String tempDir;

    public void init() {
        String sysTemp = System.getProperty("java.io.tmpdir");
        if(sysTemp.endsWith(File.separator)) {
            tempDir = sysTemp + BaseConstants.BIOSECURITY_DLL_FILE;
        } else {
            tempDir = sysTemp + File.separator + BaseConstants.BIOSECURITY_DLL_FILE;
        }

        File f = new File(tempDir);
        if(!f.exists()) {
            f.mkdir();
        }
        addJavaLibPath(tempDir);
    }

    /**
     * addJavaLibPath 
     * At first, use  java.library.path. But it doesn't works with MAC.
     * @param tempDir
     */
    private void addJavaLibPath(String tempDir) {
        System.setProperty("jna.library.path", tempDir);
    }

    public void copyToJavaLibraryPath() {
        String path = getOs() + "/" + getArch() + "/" + "**"+ "/" +"*.*";
        Resource[] resources = ResourceUtil.loadResources(path);
        for (Resource resource:resources) {
            copyResourceToDir(resource, tempDir);
        }
    }

    public void loadLibrary(String dllName) {
        String fileName = tempDir + File.separator + dllName + "." + getOs();
        System.load(fileName);
    }

    public String getOs() {
        if (StringUtils.containsIgnoreCase(System.getProperty("os.name"), "windows")) {
            return "dll";
        }

        if (StringUtils.containsIgnoreCase(System.getProperty("os.name"), "linux")) {
            return "so";
        }

        if (StringUtils.containsIgnoreCase(System.getProperty("os.name"), "mac")) {
            return "dylib";
        }

        return "unknown";
    }

    private String getArch() {
        return System.getProperty("sun.arch.data.model");
    }

    private File copyResourceToDir(Resource dllResource, String destFileName) {
        File destFile = null;
        try {
            InputStream input = dllResource.getInputStream();

            String relativePaths[] = dllResource.getURI().toString().replaceAll("\\\\","/").split( getOs() + "/" + getArch());
            String relativePath = relativePaths[relativePaths.length - 1];
            if (StringUtils.containsIgnoreCase(System.getProperty("os.name"), "windows")) {
                relativePath = relativePath.replaceAll("/","\\\\");
            }
            destFile = new File(destFileName + relativePath);
            if (!destFile.exists()) {
                FileUtils.copyInputStreamToFile(input, destFile);
            }
            return destFile;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}