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
 * The <code>CharacterData</code> interface extends Node with a set  of 
 * attributes and methods for accessing character data in the DOM.  For 
 * clarity this set is defined here rather than on each object that uses 
 * these attributes and methods. No DOM objects correspond directly to 
 * <code>CharacterData</code>, though <code>Text</code> and others do inherit 
 * the interface from it. All <code>offset</code>s in this interface start 
 * from 0.
 *
 * @version Revision: 14 1.3 src/org/w3c/dom/CharacterData.java, parser, xml4j2, xml4j2_0_0 
 */
public interface CharacterData extends Node {
  /**
   * Append the string to the end of the character data of the node. Upon 
   * success, <code>data</code> provides access to the concatenation of 
   * <code>data</code> and the <code>DOMString</code> specified.
   * @param arg The <code>DOMString</code> to append.
   * @exception DOMException
   *   NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
   */
  public void               appendData(String arg)
									   throws DOMException;   
  /**
   * The character data of the node that implements this interface. The DOM 
   * implementation may not put arbitrary limits on the amount of data that 
   * may be stored in a  <code>CharacterData</code> node. However, 
   * implementation limits may  mean that the entirety of a node's data may 
   * not fit into a single <code>DOMString</code>. In such cases, the user 
   * may call <code>substringData</code> to retrieve the data in 
   * appropriately sized pieces.
   * @exception DOMException
   *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
   * @exception DOMException
   *   DOMSTRING_SIZE_ERR: Raised when it would return more characters than 
   *   fit in a <code>DOMString</code> variable on the implementation 
   *   platform.
   */
  public String             getData()
								 throws DOMException; 
  /**
   * The number of characters that are available through <code>data</code> and 
   * the <code>substringData</code> method below.  This may have the value 
   * zero,  i.e., <code>CharacterData</code> nodes may be empty.
   */
  public int                getLength();      
  public void               setData(String data)
								 throws DOMException; 
  /**
   * Extracts a range of data from the node.
   * @param offset Start offset of substring to extract.
   * @param count The number of characters to extract.
   * @return The specified substring. If the sum of <code>offset</code> and 
   *   <code>count</code> exceeds the <code>length</code>, then all 
   *   characters to the end of the data are returned.
   * @exception DOMException
   *   INDEX_SIZE_ERR: Raised if the specified offset is negative or greater 
   *   than the number of characters in <code>data</code>, or if the 
   *   specified <code>count</code> is negative.
   *   <br>DOMSTRING_SIZE_ERR: Raised if the specified range of text does not 
   *   fit into a <code>DOMString</code>.
   */
  public String             substringData(int offset, 
										  int count)
										  throws DOMException;  
}