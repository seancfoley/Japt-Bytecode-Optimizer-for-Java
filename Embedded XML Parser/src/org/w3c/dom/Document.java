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
 * The <code>Document</code> interface represents the entire HTML or XML 
 * document. Conceptually, it is the root of the document tree, and provides 
 * the  primary access to the document's data.
 * <p>Since elements, text nodes, comments, processing instructions, etc. 
 * cannot exist outside the context of a <code>Document</code>, the 
 * <code>Document</code> interface also contains the factory methods needed 
 * to create these objects.  The <code>Node</code> objects created have a 
 * <code>ownerDocument</code> attribute which associates them with the 
 * <code>Document</code> within whose  context they were created.
 *
 * @version Revision: 16 1.3 src/org/w3c/dom/Document.java, parser, xml4j2, xml4j2_0_0 
 */
public interface Document extends Node {
  /**
   * Creates an <code>Attr</code> of the given name. Note that the 
   * <code>Attr</code> instance can then be set on an <code>Element</code> 
   * using the <code>setAttribute</code> method. 
   * @param name The name of the attribute.
   * @return A new <code>Attr</code> object.
   * @exception DOMException
   *   INVALID_CHARACTER_ERR: Raised if the specified name contains an 
   *   invalid character.
   */
  public Attr               createAttribute(String name)
											throws DOMException;
  /**
   * Creates a <code>CDATASection</code> node whose value  is the specified 
   * string.
   * @param data The data for the <code>CDATASection</code> contents.
   * @return The new <code>CDATASection</code> object.
   * @exception DOMException
   *   NOT_SUPPORTED_ERR: Raised if this document is an HTML document.
   */
  public CDATASection       createCDATASection(String data)
											   throws DOMException;   
  /**
   * Creates an element of the type specified. Note that the instance returned 
   * implements the Element interface, so attributes can be specified 
   * directly  on the returned object.
   * @param tagName The name of the element type to instantiate. For XML, this 
   *   is case-sensitive. For HTML, the  <code>tagName</code> parameter may 
   *   be provided in any case,  but it must be mapped to the canonical 
   *   uppercase form by  the DOM implementation. 
   * @return A new <code>Element</code> object.
   * @exception DOMException
   *   INVALID_CHARACTER_ERR: Raised if the specified name contains an 
   *   invalid character.
   */
  public Element            createElement(String tagName)
										  throws DOMException;  
  /**
   * Creates a <code>ProcessingInstruction</code> node given the specified 
   * name and data strings.
   * @param target The target part of the processing instruction.
   * @param data The data for the node.
   * @return The new <code>ProcessingInstruction</code> object.
   * @exception DOMException
   *   INVALID_CHARACTER_ERR: Raised if an invalid character is specified.
   *   <br>NOT_SUPPORTED_ERR: Raised if this document is an HTML document.
   */
  public ProcessingInstruction createProcessingInstruction(String target, 
														   String data)
														   throws DOMException;   
  /**
   * Creates a <code>Text</code> node given the specified string.
   * @param data The data for the node.
   * @return The new <code>Text</code> object.
   */
  public Text               createTextNode(String data);      
  /**
   * This is a convenience attribute that allows direct access to the child 
   * node that is the root element of  the document. For HTML documents, this 
   * is the element with the tagName "HTML".
   */
  public Element            getDocumentElement();      
  /**
   * Returns a <code>NodeList</code> of all the <code>Element</code>s with a 
   * given tag name in the order in which they would be encountered in a 
   * preorder traversal of the <code>Document</code> tree. 
   * @param tagname The name of the tag to match on. The special value "*" 
   *   matches all tags.
   * @return A new <code>NodeList</code> object containing all the matched 
   *   <code>Element</code>s.
   */
  public NodeList           getElementsByTagName(String tagname);      
}