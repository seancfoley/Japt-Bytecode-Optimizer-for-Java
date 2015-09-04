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
 * The <code>Text</code> interface represents the textual content (termed 
 * character  data in XML) of an <code>Element</code> or <code>Attr</code>.  
 * If there is no markup inside an element's content, the text is contained 
 * in a single object implementing the <code>Text</code> interface that is 
 * the only child of the element. If there is markup, it is parsed into a 
 * list of elements and <code>Text</code> nodes that form the list of 
 * children of the element.
 * <p>When a document is first made available via the DOM, there is  only one 
 * <code>Text</code> node for each block of text. Users may create  adjacent 
 * <code>Text</code> nodes that represent the  contents of a given element 
 * without any intervening markup, but should be aware that there is no way 
 * to represent the separations between these nodes in XML or HTML, so they 
 * will not (in general) persist between DOM editing sessions. The 
 * <code>normalize()</code> method on <code>Element</code> merges any such 
 * adjacent <code>Text</code> objects into a single node for each block of 
 * text; this is  recommended before employing operations that depend on a 
 * particular document structure, such as navigation with 
 * <code>XPointers.</code> 
 *
 * @version Revision: 29 1.3 src/org/w3c/dom/Text.java, parser, xml4j2, xml4j2_0_0 
 */
public interface Text extends CharacterData {
}