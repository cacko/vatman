package com.mutanti.vatman.Exception;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

public class TerminateSaxParsing extends SAXParseException {

	public TerminateSaxParsing(String message, Locator locator) {
		super(message, locator);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5857215722576179263L;
	
}