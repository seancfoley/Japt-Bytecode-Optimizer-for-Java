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
 * The <code>Attr</code> interface represents an attribute in an 
 * <code>Element</code> object.Typically the allowable values for the 
 * attribute are defined in a documenttype definition.
 * <p><code>Attr</code> objects inherit the <code>Node</code>  interface, but 
 * since they are not actually child nodes of the element  they describe, the 
 * DOM does not consider them part of the document  tree.  Thus, the 
 * <code>Node</code> attributes <code>parentNode</code>,  
 * <code>previousSibling</code>, and <code>nextSibling</code> have a  null 
 * value for <code>Attr</code> objects. The DOM takes the  view that 
 * attributes are properties of elements rather than having a  separate 
 * identity from the elements they are associated with;  this should make it 
 * more efficient to implement such features as default attributes associated 
 * with all elements of a  given type.  Furthermore, <code>Attr</code> nodes 
 * may not be immediate children of a <code>DocumentFragment</code>. However, 
 * they can be associated with <code>Element</code> nodes contained within a 
 * <code>DocumentFragment</code>. In short, users and implementors of the DOM 
 * need to be aware that  <code>Attr</code> nodes have some things in  common 
 * with other objects inheriting the <code>Node</code> interface,  but they 
 * also are quite distinct.
 * <p> The attribute's effective value is determined as follows: if this  
 * attribute has been explicitly assigned any value, that value is the  
 * attribute's effective value; otherwise, if there is a declaration for  
 * this attribute, and that declaration includes a default value, then  that 
 * default value is the attribute's effective value; otherwise, the  
 * attribute does not exist on this element in the structure model until  it 
 * has been explicitly added.  Note that the <code>nodeValue</code>  
 * attribute on the <code>Attr</code> instance can also be used to retrieve 
 * the string version of the attribute's value(s). 
 * <p>In XML, where the value of an attribute can contain entity references, 
 * the child nodes of the <code>Attr</code> node provide a representation in 
 * which entity references are not expanded. These child nodes may be either 
 * <code>Text</code> or <code>EntityReference</code> nodes. Because the 
 * attribute type may be unknown, there are no tokenized attribute values.
 *
 * @version Revision: 22 1.3 src/org/w3c/dom/Attr.java, parser, xml4j2, xml4j2_0_0 
 */
public interface Attr extends Node {
  /**
   * Returns the name of this attribute. 
   */
  public String             getName();      
  /**
   * On retrieval, the value of the attribute is returned as a string. 
   * Character and general entity references are replaced with their values.
   * <br>On setting, this creates a <code>Text</code> node with the unparsed 
   * contents of the string.
   */
  public String             getValue();      
  public void               setValue(String value);      
}