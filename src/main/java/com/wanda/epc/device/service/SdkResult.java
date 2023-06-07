package com.wanda.epc.device.service;

public class SdkResult {
	/**
	 * sdk调用结果信息
	 */
	private int result;
	/**
	 * 结果内容
	 */
	private String data;
	
	/**
	 * 成功或失败
	 */
	private boolean success;

	public SdkResult() {
		super();
	}

	public SdkResult(int result, String data) {
		super();
		this.result = result;
		this.data = data;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
		setSuccess(result>=0);
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public boolean getSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	public String toString() {
		return "SdkResult [result=" + result + ", data=" + data + ", success=" + success + "]";
	}

}
