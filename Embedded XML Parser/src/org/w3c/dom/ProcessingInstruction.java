package org.w3c.dom;

/** 
 * ----- 
 * Copyright (c) World Wide Web Consortium, (Massachusetts Institute of 
 * Technology, Institut National de Recherche en Informatique et en 
 * Automatique, Keio University). All Rights Reserved. 
 * http://www.w3.org/Consortium/Legal/ 
 * ----- 
 */ 

/**
 * The <code>ProcessingInstruction</code> interface represents a  "processing 
 * instruction", used in XML as a way to keep processor-specific information 
 * in the text of the document.
 *
 * @version Revision: 28 1.3 src/org/w3c/dom/ProcessingInstruction.java, parser, xml4j2, xml4j2_0_0 
 */
public interface ProcessingInstruction extends Node {
  /**
   * The content of this processing instruction. This is from the first non 
   * white space character after the target to the character immediately 
   * preceding the <code>?&gt;</code>.
   * @exception DOMException
   *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
   */
  public String             getData();      
  /**
   * The target of this processing instruction. XML defines this as being the 
   * first token following the markup that begins the processing instruction.
   */
  public String             getTarget();      
  public void               setData(String data)
									  throws DOMException;  
}