package com.ibm.ive.tools.japt.assembler;
/*
 * (c) Copyright 2003 IBM.
 * All Rights Reserved.
 */

/**
 * Very simple scanner. Read a byte stream, remove comments, read one word at a time,
 * remember what line we're on.
 */
public class Scanner 
{
	int index = 0;
	StringBuffer buffer;
	public int lineNumber = 1;
	boolean eof = false;
	String fileName; //the file name including its path
	
    public Scanner(byte bytes[], String fileName) {
    	this.fileName = fileName;
    	this.buffer = new StringBuffer(new String(bytes));
    	stripComments(buffer);
    }
    
	void stripComments(StringBuffer buffer) {
		int max = buffer.length()-1;
		for (int n=0; n<max; n++) {
			if (buffer.charAt(n) == '"') {
				//skip string
				n++;
				while (true) {
					if(n >= max) {
						break;
					}
					char c = buffer.charAt(n);
					if (c == '"')
						break;
					if (c == '\\') {
						n++;
					}
					n++;
				}
			}
			else if (buffer.charAt(n) == '/') {
				if (buffer.charAt(n+1) == '/') {
					while (n<max && buffer.charAt(n)!='\n') {
						buffer.setCharAt(n++, ' ');
					}
				}
				else if (buffer.charAt(n+1) == '*') {
					buffer.setCharAt(n++, ' ');
					buffer.setCharAt(n++, ' ');
					while (n<max-1 && (buffer.charAt(n)!='*' || buffer.charAt(n+1)!='/')) {
			        	if (buffer.charAt(n) == '\n') {
			        		//lineNumber++;
			        		n++;
			        	}
			        	else {
			        		buffer.setCharAt(n++, ' ');
			        	}
			        }
					buffer.setCharAt(n++, ' ');
					buffer.setCharAt(n++, ' ');
				}
			}
		}					
	}
    
    public boolean hasMoreTokens() {
    	return !eof;
    }

    public String getToken() {
    	try {
	        skipSpaces();
	        if (buffer.charAt(index) == '"') {
	        	String str = readString();
		    	//if (Parser.DEBUG) System.err.println("-->"+str+"<--");
	        	return str;
	        }
	        else if (isDelimiter(buffer.charAt(index))) {
		    	//if (Parser.DEBUG) System.err.println("-->"+buffer.charAt(index)+"<--");
	        	return buffer.substring(index++,index);
	        }
	        StringBuffer sbuf = new StringBuffer();
	        
	        while (hasMoreTokens()) {
	            sbuf.append(buffer.charAt(index));
	            index++;
	            if (isDelimiter(buffer.charAt(index))) {
	            	break;
	            }
	        }
	    	//if (Parser.DEBUG) System.err.println("-->"+sbuf.toString()+"<--");
			return sbuf.toString();
    	}
    	catch (Exception e) {
    		//if (Parser.DEBUG) System.err.println("Scanner reached end of input buffer");
    		eof = true;
    		return null;
    	}
    }

    
    
	private String readString() {
		index++;
		StringBuffer buf = new StringBuffer();
		while (hasMoreTokens()) {
			char c = buffer.charAt(index);
			if (c == '"')
				break;
			if (c == '\\') {
				index++;
				char c2 = buffer.charAt(index);
				switch(c2) {
					case 'r':
						buf.append('\r');
						break;
					case 'n':
						buf.append('\n');
						break;
					case 't':
						buf.append('\t');
						break;
					case 'b':
						buf.append('\b');
						break;
					case 'f':
						buf.append('\f');
						break;
					default:
						buf.append(c2);
				}
			}
			else {
				buf.append(c);
			}
			index++;
		}
		index++;
		return buf.toString();
	}


    void skipSpaces() {
    	char c;
        while (hasMoreTokens() && isSpace(c = buffer.charAt(index))) {
        	if (c == '\n')
        		lineNumber++;
            index++;
        }
    }

	private boolean isDelimiter(char c) {
		switch(c) {
			case '"':
			case ':':
			case '(':
			case ')':
			case '{':
			case '}':
			case ',':
			case ';':
			case '[':
			case ']':			
				return true;
			default:
				return isSpace(c);
		}
	}

	private boolean isSpace(char c) {
		switch(c) {
			case ' ':
			case '\r':
			case '\n':
			case '\t':
				return true;
			default:
				return false;
		}
	}
	
}
