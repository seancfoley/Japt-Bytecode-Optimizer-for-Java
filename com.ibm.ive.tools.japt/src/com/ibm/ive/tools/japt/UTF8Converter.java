package com.ibm.ive.tools.japt;

/**
 * standard conversion of strings from Java Unicode to UTF8 and vice-versa
 * @author sfoley
 */
public class UTF8Converter {

	public static String convertToString(byte[] bytes) {
		char[] chars = new char[bytes.length];
		int position = 0;
		int charPosition = 0;
		while (position < bytes.length) {
			int byte1 = bytes[position++];
			int byte2, byte3;
			
			if (0 == (0x80 & byte1)) {
				chars[charPosition++] = (char) byte1;
			}
			else if (0xC0 == (0xE0 & byte1)) {
				byte2 = bytes[position++];
				chars[charPosition++] = (char) (((byte1 & 0x1F) << 6) | (byte2 & 0x3F));
			}
			else {
				byte2 = bytes[position++];
				byte3 = bytes[position++];
				chars[charPosition++] = (char) (((byte1 & 0x0F) << 12) | ((byte2 & 0x3F) << 6) | (byte3 & 0x3F));
			}
		}
	
		char[] resultChars = new char[charPosition];
		System.arraycopy(chars,0,resultChars,0,resultChars.length);
		return new String(resultChars);
	}
	
	/**
	Convert a String to UTF8 bytes.
	<xmp>(c) Copyright IBM Corp 1998</xmp>
	*/
	public static byte[] convertToUtf8(String string) {
		char[] chars = new char[string.length()];
		byte[] bytes = new byte[3 * chars.length];
		string.getChars(0,chars.length,chars,0);
		int position = 0;
		for (int i=0; i<chars.length; i++) {
			char aChar = chars[i];
			
			if      ((aChar > 0) && (aChar < 0x007F)) {
				bytes[position++] = (byte) (0xFF & aChar);
			}
			else if (aChar < 0x07FF) {
				bytes[position++] = (byte) (0xC0 | (0x1f & (aChar >> 6)));
				bytes[position++] = (byte) (0x80 | (0x3f & (aChar)));
			}
			else {
				bytes[position++] = (byte) (0xE0 | (0x0f & (aChar >> 12)));
				bytes[position++] = (byte) (0x80 | (0x3f & (aChar >>  6)));
				bytes[position++] = (byte) (0x80 | (0x3f & (aChar)));
			}
		}
	
		byte[] bytesResult = new byte[position];
		System.arraycopy(bytes,0,bytesResult,0,bytesResult.length);
		return bytesResult;
	}

}
