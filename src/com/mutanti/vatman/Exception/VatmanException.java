package com.mutanti.vatman.Exception;

public class VatmanException extends Exception {

	private static final long serialVersionUID = 5838516338855262397L;
	public static final int INVALID_RESPONSE = 1;
	public static final int SCHEDULE_IS_EMPTY = 2;
	public static final int INVALID_XML_RESPONSE = 3;
	public static final int CONNECTION_FAILED = 4;
	public static final int SERVICE_NOT_AVAILABLE = 5;
	public static final int ACCESS_DENIED = 6;
	public static final int SERVER_MESSAGE = 7;
	private int errNo = 0; 

	public VatmanException(int code, String msg) {
		super(msg);
		errNo = code;
	}

	final public int getErrNo() {
		return errNo;
	}

}
