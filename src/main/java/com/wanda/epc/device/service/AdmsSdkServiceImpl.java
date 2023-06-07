package com.wanda.epc.device.service;

import org.apache.commons.lang3.StringUtils;
import zk.jni.JavaToAdmsPullSDK;

import java.util.ArrayList;
import java.util.List;

public class AdmsSdkServiceImpl {

    private static int LOADLIBRARY_FAILURE = -126;
	private static LibraryService libraryService = new LibrarayServiceImpl();

	/**
	 * 判断操作系统是否是window
	 * @return
	 */
	public static  boolean isWindow() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}

	static{
		libraryService.init();
		libraryService.copyToJavaLibraryPath();
		if (isWindow()) {
			libraryService.loadLibrary("plcommpro");
			libraryService.loadLibrary("plcomms");
			libraryService.loadLibrary("plrscagent");
			libraryService.loadLibrary("plrscomm");
			libraryService.loadLibrary("pltcpcomm");
			libraryService.loadLibrary("plusbcomm");
		}
	}

	
	public SdkResult connect(String connectParam) {
		SdkResult sdkResult = new SdkResult();
		try
		{
			int result = (int) JavaToAdmsPullSDK.Connect(connectParam);
			if(result > 0) {
				sdkResult.setResult(result);
				sdkResult.setData(result+"");
			} else {
				int error = pullLastError().getResult();
				if(error >= 0) {
					sdkResult.setResult(0 - error);
					sdkResult.setData((0 - error)+"");
				}
			}
		}
		catch (Error e)
		{
			sdkResult.setResult(LOADLIBRARY_FAILURE);
			sdkResult.setData(LOADLIBRARY_FAILURE + "");
		}
		return sdkResult;
	}

	
	public SdkResult connectExt(String connectParam, int[] callbackResult) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = (int) JavaToAdmsPullSDK.ConnectExt(connectParam, callbackResult);
			if (result <= 0) {
				sdkResult.setResult(callbackResult[0]);
				sdkResult.setData(callbackResult[0] + "");
			} else {
				sdkResult.setResult(result);
				sdkResult.setData(result + "");
			}
		} catch (Error e) {
				sdkResult.setResult(LOADLIBRARY_FAILURE);
				sdkResult.setData(LOADLIBRARY_FAILURE + "");
		}
		return sdkResult;
	}

	
	public SdkResult disconnect(long hCommPro) {
		SdkResult sdkResult = new SdkResult();
		try {
			if (hCommPro > 0) {
				int result = JavaToAdmsPullSDK.Disconnect(hCommPro);
				sdkResult.setResult(result);
				sdkResult.setData(result + "");
			}
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
			sdkResult.setData(LOADLIBRARY_FAILURE + "");
		}
		return sdkResult;
	}

	
	public SdkResult setDeviceParam(long hCommPro, String items) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.SetDeviceParam(hCommPro, items);
			sdkResult.setResult(result);
			sdkResult.setData("");
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult getDeviceParam(long hCommPro, byte[] callbackResult, String items) {
		SdkResult sdkResult = new SdkResult();
		try
		{
			String[] itemArray = items.split(",");// 参数数量
			List<String> itemList = new ArrayList<String>();
			if(itemArray.length > 25)// 获取参数，一次获取的参数个数有限制：30个参数，需要分次发送
			{
				StringBuffer strBuf = new StringBuffer();
				for(int i = 0; i < itemArray.length; i++) {
					strBuf.append(itemArray[i] + ",");
					if(i != 0 && i % 25 == 0) {
						itemList.add(strBuf.toString().substring(0, strBuf.length()-1));
						strBuf = new StringBuffer();
					}
				}
				if(strBuf.length() > 0) {
					itemList.add(strBuf.toString().substring(0, strBuf.length()-1));
				}
			} else {
				itemList.add(items);
			}
			int result = -1;
			for(String item : itemList) {
				result = JavaToAdmsPullSDK.GetDeviceParam(hCommPro, callbackResult, item);
				if(result >= 0) {
					sdkResult.setData(StringUtils.isNotBlank(sdkResult.getData()) ? sdkResult.getData() + "," + new String(callbackResult).trim() : new String(callbackResult).trim());
					sdkResult.setResult(result);
					sdkResult.setSuccess(true);
				}
			}
			if(StringUtils.isBlank(sdkResult.getData())) {
				sdkResult.setData("");
				sdkResult.setResult(result);
				sdkResult.setSuccess(true);
			}
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult controlDevice(long hCommPro, int operationID, int param1, int param2, int param3, int param4,
			String options) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.ControlDevice(hCommPro, operationID,  param1, param2, param3, param4, options);
			sdkResult.setResult(result);
			sdkResult.setData("");
			sdkResult.setSuccess(true);
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult setDeviceData(long hCommPro, String tableName, String data, String options) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.SetDeviceData(hCommPro, tableName, data, options);
			sdkResult.setResult(result);
			sdkResult.setData("");
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult getDeviceData(long hCommPro, byte[] callbackResult, String tableName, String fieldNames, String filter,
			String options) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.GetDeviceData(hCommPro, callbackResult, tableName, fieldNames, filter, options);
			sdkResult.setResult(result);
			sdkResult.setData(new String(callbackResult,"UTF-8").trim());
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		} catch (Exception e) {
		}
		return sdkResult;
	}

	
	public SdkResult getDeviceDataCount(long hCommPro, String tableName, String filter, String options) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.GetDeviceDataCount(hCommPro, tableName, filter, options);
			sdkResult.setResult(result);
			sdkResult.setData("");
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult deleteDeviceData(long hCommPro, String tableName, String data, String options) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.DeleteDeviceData(hCommPro, tableName, data, options);
			sdkResult.setResult(result);
			sdkResult.setData("");
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult getRTLog(long hCommPro, byte[] callbackResult) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.GetRTLog(hCommPro, callbackResult);
			sdkResult.setResult(result);
			if(result >= 0) {
				sdkResult.setData(new String(callbackResult).trim()); 
			} else {
				sdkResult.setData("");
			}
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult getRTLogExt(long hCommPro, byte[] callbackResult) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.GetRTLogExt(hCommPro, callbackResult);
			sdkResult.setResult(result);
			if(result >= 0) {
				sdkResult.setData(new String(callbackResult).trim());
			} else {
				sdkResult.setData("");
			}
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult searchDevice() {
		int bufferSize = 1024 * 1024;// 1M内存
		byte[] callbackResult = new byte[bufferSize];
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.SearchDevice("UDP", "255.255.255.255", callbackResult);
			sdkResult.setResult(result);
			if(result > 0) {
				sdkResult.setData(new String(callbackResult).trim());
			}
		} catch (Error e) {
			e.printStackTrace();
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult modifyIPAddress(String buffer) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.ModifyIPAddress("UDP","255.255.255.255",buffer);
			sdkResult.setResult(result);
			sdkResult.setData("");
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult pullLastError() {
		SdkResult sdkResult = new SdkResult();
		try {
		sdkResult.setResult(JavaToAdmsPullSDK.PullLastError());
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult setDeviceFileData(long hCommPro, String fileName, String buffer, int bufferSize, String options) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.SetDeviceFileData(hCommPro, fileName,buffer,bufferSize,options);
			sdkResult.setResult(result);
			sdkResult.setData("");
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult getDeviceFileData(long hCommPro, byte[] callbackResult, String fileName, String options) {
		SdkResult sdkResult = new SdkResult();
		try {
				int result = JavaToAdmsPullSDK.GetDeviceFileData(hCommPro, callbackResult, fileName, options);
			sdkResult.setResult(result);
			if(result >= 0) {
				sdkResult.setData(new String(callbackResult).trim());
			} else {
				sdkResult.setData("");
			}
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult processBackupData(byte[] revBuf, int fileLen, byte[] callbackResult, int outSize) {
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.ProcessBackupData(revBuf, fileLen, callbackResult, outSize);
			sdkResult.setResult(result);
			if(result >= 0) {
				sdkResult.setData(new String(callbackResult).trim());
			} else {
				sdkResult.setData("");
			}
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}

	
	public SdkResult searchDeviceEx(String paramItems) {
		int bufferSize = 1024 * 1024;// 1M内存
		byte[] callbackResult = new byte[bufferSize];
		SdkResult sdkResult = new SdkResult();
		try {
			int result = JavaToAdmsPullSDK.SearchDeviceEx("UDP", "255.255.255.255", paramItems,callbackResult);
			sdkResult.setResult(result);
			if(result > 0) {
				sdkResult.setData(new String(callbackResult).trim());
			}
		} catch (Error e) {
			sdkResult.setResult(LOADLIBRARY_FAILURE);
		}
		return sdkResult;
	}
	
}
