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
 * The <code>NodeList</code> interface provides the abstraction of an ordered 
 * collection of nodes, without defining or constraining how this collection 
 * is implemented.
 * <p>The items in the <code>NodeList</code> are accessible via an integral 
 * index, starting from 0. 
 *
 * @version Revision: 26 1.3 src/org/w3c/dom/NodeList.java, parser, xml4j2, xml4j2_0_0 
 */
public interface NodeList {
  /**
   * The number of nodes in the list. The range of valid child node indices is 
   * 0 to <code>length-1</code> inclusive. 
   */
  public int                getLength();      
  /**
   * Returns the <code>index</code>th item in the collection. If 
   * <code>index</code> is greater than or equal to the number of nodes in 
   * the list, this returns <code>null</code>.
   * @param index Index into the collection.
   * @return The node at the <code>index</code>th position in the 
   *   <code>NodeList</code>, or <code>null</code> if that is not a valid 
   *   index.
   */
  public Node               item(int index);      
}