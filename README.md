# Japt Bytecode Optimizer for Java
Bytecode optimization tool to ease development and deployment of Java

Description
The tools provide the following technologies:

* escape analysis on a per method-invocation basis 
* static control flow analysis that determines entry points into an application or library
* removal of unwanted attributes from Java™ class files
* addition or rewrite of stackmaps for Java class files
* verification of Java class files
* auto-generation of classes to load all other classes within the same archive 
* specific remapping of method invocations
* specific rewrites of bytecodes
* obfuscation of class, field and method names
* assembler and disassembler of bytecodes
* static control flow analysis for removal of unused methods, classes, fields
* static control flow analysis that splits an application into archives according to thread accessibility
