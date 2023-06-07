package zk.jni;

/**
 * !!!!!!!!!!!!!!!!!
 * 请注意 这个类务必放在  zk.jni包下，不可放在自定义包下!!!!!!!!
 */
public class JavaToAdmsPullSDK {
	/**
	 * 连接设备，连接成功后返回连接句柄
	 */
	public static native long Connect(String connectParam);
	
	/**
	 * 连接设备，连接成功后返回连接句柄
	 */
	
	public static native long ConnectExt(String connectParam, int[] callbackResult);

	/**
	 * 断开与设备的连接
	 */
	public static native int Disconnect(long hcommpro);

	/**
	 * 设置控制器参数，例如设备号、门磁类型、锁驱动时间、读卡间隔等
	 */
	public static native int SetDeviceParam(long hcommpro, String items);

	/**
	 * 读取控制器参数，例如设备号、门磁类型、锁驱动时间、读卡间隔等
	 */
	public static native int GetDeviceParam(long hcommpro, byte[] callbackResult, String item);

	/**
	 * 控制控制器动作
	 */
	public static native int ControlDevice(long hcommpro, int operationID, int param1, int param2, int param3, int param4, String options);

	/**
	 * 设置数据到设备
	 */
	public static native int SetDeviceData(long hcommpro, String tableName, String data, String options);

	/**
	 * 从设备读取数据
	 */
	public static native int GetDeviceData(long hcommpro, byte[] callbackResult, String tableName, String fieldNames, String filter, String options);

	/**
	 * 读取设备中的记录总数信息，返回指定数据的记录条数
	 */
	public static native int GetDeviceDataCount(long hcommpro, String tableName, String filter, String options);

	/**
	 * 删除设备中的数据，例如用户信息、时间段等数据
	 */
	public static native int DeleteDeviceData(long hcommpro, String tableName, String data, String options);

	/**
	 * 获取设备产生的实时事件记录以及设备的门状态、报警状态等
	 */
	public static native int GetRTLog(long hcommpro, byte[] callbackResult);
	
	public static native int GetRTLogExt(long hcommpro, byte[] callbackResult);
	
	/**
	 * 搜索局域网内的门禁控制器
	 * 
	 */
	public static native int SearchDevice(String commType, String address, byte[] callbackResult);

	/**
	 * UDP广播方式修改控制器IP地址
	 */
	public static native int ModifyIPAddress(String commType, String address, String buffer);

	/**
	 * 获取返回值错误码
	 */
	public static native int PullLastError();

	/**
	 * 将文件从PC传送到设备
	 */
	public static native int SetDeviceFileData(long hcommpro, String fileName, String buffer, int bufferSize, String options);

	/**
	 * 从设备获取文件到PC
	 */
	public static native int GetDeviceFileData(long hcommpro, byte[] callbackResult, String fileName, String options);

	/**
	 * 用来处理设备备份的文件，如SD卡中的备份文件等
	 */
	public static native int ProcessBackupData(byte[] revBuf, int fileLen, byte[] callbackResult, int outSize);
	
	/**
	 * 广播获取参数
	 */
	public static native int SearchDeviceEx(String commType, String address, String paramItems, byte[] callbackResult);
	
}
