package com.ibm.ive.tools.japt.assembler;
/*
 * (c) Copyright 2003 IBM.
 * All Rights Reserved.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptClass;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_AttributeVector;
import com.ibm.jikesbt.BT_BasicBlockMarkerIns;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassFileException;
import com.ibm.jikesbt.BT_ClassVersion;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_DuplicateClassException;
import com.ibm.jikesbt.BT_ExceptionTableEntry;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_LineNumberAttribute;
import com.ibm.jikesbt.BT_LineNumberAttribute.DereferencedPCRange;
import com.ibm.jikesbt.BT_LineNumberAttribute.PcRange;
import com.ibm.jikesbt.BT_LookupSwitchIns;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_NoSuchMemberException.BT_NoSuchFieldException;
import com.ibm.jikesbt.BT_NoSuchMemberException.BT_NoSuchMethodException;
import com.ibm.jikesbt.BT_Opcodes;
import com.ibm.jikesbt.BT_Repository;
import com.ibm.jikesbt.BT_TableSwitchIns;

/**
 * With a provided {@link Scanner}, read a byte stream and parse the characters that
 * form the assembly file. A JikesBT class is made for each class found, and
 * {@link BT_Method}'s and {@link BT_Field}'s are added when discovered in the assembly file.
 * While the methods in the assembly file are parsed, {@link BT_Ins} instances are created
 * and appended to the current method. In the end, the resulting class file is 
 * written out to disk and the file can be dumped to compare it with the original input.
 * <p>
 * This is a recursive descent parser. No grammar was used as the syntax is trivial, and
 * there is only one conflict that requires 3 look-ahead tokens (see 
 * {@link Parser.readFieldOrMethod} to decypher whether something is a method declaration
 * or a field declaration.
 * <p>
 * There are lots of String comparisons in this code. A symbol table with integer comparison
 * would have been faster, but assembly files are not compiled too often, so speed was not
 * a driving factor in the implementation of this parser (time to market was more important).
 */
public class Parser implements BT_Opcodes {
	private final Scanner scanner;
	String token;
	String storedToken;
	private final JaptRepository repository;
	private final BT_ClassVersion classVersion;
	ClassPathEntry classPathEntry;
	
	final static boolean DEBUG = false;
	//final static boolean DEBUG = true;
	
	public Parser(
			JaptRepository repository,
			ClassPathEntry entry,
			Scanner scanner,
			BT_ClassVersion classVersion) {
		this.classPathEntry = entry;
		this.repository = repository;
		this.scanner = scanner;
		this.classVersion = classVersion;
	}
	
	public JaptClass parse(JaptClass stub) 
			throws UnexpectedTokenException, 
				BT_DuplicateClassException, 
				BT_ClassFileException,
				InvalidIdentifierException {
		InfoUntilName info = readUntilName();
		return readClassDeclaration(info, stub);
	}
	
	public String parseName() throws UnexpectedTokenException {
		return readUntilName().name;
	}

	private static class InfoUntilName {
		short modifiers;
		boolean isInterface;
		String name;
	}
	
	/////////////////////////////////////////////////////////////////////////
	//   Class declaration parse methods
	/////////////////////////////////////////////////////////////////////////
	
	public InfoUntilName readUntilName() throws UnexpectedTokenException {
		InfoUntilName info = new InfoUntilName();
		info.modifiers = readModifiers();
		if (!scanner.hasMoreTokens())
			return null;
		expect(new String[] {BT_Class.CLASS_NAME, BT_Class.INTERFACE_NAME});
		info.isInterface = token.equals(BT_Class.INTERFACE_NAME);
		
		if(info.isInterface) {
			info.modifiers = (short) (info.modifiers | BT_Class.INTERFACE);
		}
		info.name = nextToken();
		return info;
	}
	
	private JaptClass readClassDeclaration(InfoUntilName info, JaptClass cls) 
			throws UnexpectedTokenException, 
				BT_DuplicateClassException, 
				BT_ClassFileException, 
				InvalidIdentifierException {
		
		/* note: it would have been nice if we had already created the stub.
		 * But we really cannot until we have confirmed the correct class name,
		 * which must come from the InfoUntilName object.
		 */
		if(cls == null) {
			cls = repository.getClass(info.name);
		}
		if (cls == null) {
			if(!repository.canCreate(info.name)) {
				throw new BT_DuplicateClassException(cls.getName(), cls);
			}
			if(info.isInterface) {
				cls = repository.createInternalInterface(
						new Identifier(info.name),
						classPathEntry,
						classVersion);
			} else {
				cls = repository.createInternalClass(
						new Identifier(info.name),
						classPathEntry,
						classVersion);
			}
		} else if (!cls.isStub()) {
			throw new BT_DuplicateClassException(cls.getName(), cls);
		} else {
			repository.createInternalClass(
				classPathEntry,
				info.isInterface,
				classVersion,
				cls);
		}
		
		/* 
		 * much like in BT_Repository, while we are trying to load a class we hold the table
		 * lock until the stub for the class has been inserted into the class table.
		 * At that point in time, we can release the table lock.  If we intend to load the class,
		 * we must acquire the class lock.  By acquiring the class lock before releasing the table lock,
		 * we can load the class (otherwise we'd need to check if class loaded already).
		 * 
		 */
		cls.acquireClassLock();
		repository.releaseTableLock(cls);
		try {
			cls.setFlags(info.modifiers);
			cls.setInProject(repository.getFactory().isProjectClass(info.name, null));
			//if (DEBUG) System.out.println("reading class "+cls);
			
			nextToken();
			readSuperClass(cls, info.isInterface);
			readInterfaces(cls, info.isInterface);
			readClassBody(cls);
			removeStubFieldsAndMethods(cls);
			
			//creates the source file attribute
			cls.setSourceFile(new File(scanner.fileName).getName());
		} finally {
			cls.releaseClassLock();
		}
		return cls;
	}

	/**
	 * As a result of the parsing process, stub methods and fields are generated.
	 * In this phase, we remove them again.
	 * @param cls the class to be cleaned up
	 */
	private void removeStubFieldsAndMethods(BT_Class cls) {
		for (int n=cls.fields.size()-1; n>=0; n--) {
			if (cls.fields.elementAt(n).isStub())
				cls.fields.removeElementAt(n);
		}
		for (int n=cls.methods.size()-1; n>=0; n--) {
			if (cls.methods.elementAt(n).isStub())
				cls.methods.removeElementAt(n);
		}
	}


	/////////////////////////////////////////////////////////////////////////
	//   Class header parse methods
	/////////////////////////////////////////////////////////////////////////
	//TODO match up extends and implments strings with jikesbt
	private void readSuperClass(BT_Class cls, boolean isInterface) {
		String superClass;
		if (!isInterface && token.equals("extends")) {
			superClass = nextToken();
			nextToken();
		} else {
			superClass = "java.lang.Object";
		}
		//if (DEBUG) System.out.println("superClass: "+superClass);
		if(!isInterface && !cls.getName().equals("java.lang.Object")) {
			//in JIKESBT interfaces and java.lang.Object have a null superclass 
			cls.setSuperClass(repository.forName(superClass));
		}
	}

	private void readInterfaces(BT_Class cls, boolean isInterface) {
		if (isInterface ? token.equals("extends") : token.equals("implements")) {
			token = nextToken();
			Vector names = readIdentifierList();
			for (int n=0; n<names.size(); n++) {
				BT_Class i = repository.forName((String)names.elementAt(n));
				i.becomeInterface();
				cls.getParents().addElement(i);
			}	
			if (DEBUG) System.out.println("implements: "+cls.getParents());
		}
	}

	private short readModifiers() {
		nextToken();
		if (scanner.eof)
			return 0;
		short flags = 0;
		while (true) {
			if (token.equals(BT_Item.SUPER_NAME)) 		flags |= BT_Item.SUPER; else
			if (token.equals(BT_Item.ABSTRACT_NAME)) 		flags |= BT_Item.ABSTRACT; else
			if (token.equals(BT_Item.FINAL_NAME)) 		flags |= BT_Item.FINAL; else
			if (token.equals(BT_Item.NATIVE_NAME)) 		flags |= BT_Item.NATIVE; else
			if (token.equals(BT_Item.PRIVATE_NAME)) 		flags |= BT_Item.PRIVATE; else
			if (token.equals(BT_Item.PROTECTED_NAME)) 	flags |= BT_Item.PROTECTED; else
			if (token.equals(BT_Item.PUBLIC_NAME)) 		flags |= BT_Item.PUBLIC; else
			if (token.equals(BT_Item.STATIC_NAME)) 		flags |= BT_Item.STATIC; else
			if (token.equals(BT_Item.SYNCHRONIZED_NAME)) 	flags |= BT_Item.SYNCHRONIZED; else
			if (token.equals(BT_Item.TRANSIENT_NAME)) 	flags |= BT_Item.TRANSIENT; else
			if (token.equals(BT_Item.VOLATILE_NAME)) 		flags |= BT_Item.VOLATILE; else
			if (token.equals(BT_Item.VARARGS_NAME)) 		flags |= BT_Item.VARARGS; else
			if (token.equals(BT_Item.BRIDGE_NAME)) 		flags |= BT_Item.BRIDGE; else
			if (token.equals(BT_Item.SYNTHETIC_NAME)) 	flags |= BT_Item.SYNTHETIC; else
			if (token.equals(BT_Item.STRICT_NAME)) 	flags |= BT_Item.STRICT; else
					break;
			token = nextToken();
		}
		if (DEBUG) System.out.println("modifiers: "+flags);
		return flags;
	}
	
	/////////////////////////////////////////////////////////////////////////
	//   Class body parse methods
	/////////////////////////////////////////////////////////////////////////
	
	private void readClassBody(BT_Class cls) throws UnexpectedTokenException, BT_ClassFileException {
		try {
			expect("{");
		}
		catch (UnexpectedTokenException e) {
			throw new UnexpectedTokenException(e.line, "Class body should start with { not with "+token);
		}
		while (scanner.hasMoreTokens()) {
			readFieldOrMethod(cls);
			//xxx; the line below is broken, it does not detect end of class; resinstated in version 1.24
			//if (token.equals("}"))	// end of class 
			//	break;
		}
	}

	private String appendArrayDepth(String type) throws UnexpectedTokenException {
		while(token.equals("[")) {
			nextToken();
			expect("]");
			type += "[]";
			nextToken();
		}
		return type;
	}
	
	private void readFieldOrMethod(BT_Class cls) throws UnexpectedTokenException, BT_ClassFileException {
		short modifiers = readModifiers();
		String type = token;
		nextToken();
		if (scanner.eof)
			return;
		type = appendArrayDepth(type);
		putBackToken();
		if (DEBUG) System.out.println("type="+type);
		String name = nextToken();
		if (scanner.eof)
			return;
		
							 
		if (DEBUG) System.out.println("name="+name);
		String token = nextToken();
		if (token.equals(";")) {
			// we just read a field declaration
			BT_Class typeClass = repository.forName(type);
			BT_Field field = cls.findFieldOrNull(name, typeClass);
			if(field == null) {
				field = BT_Field.createField(cls, modifiers, typeClass, name);
			}
			else if(!field.isStub()) {
				throw new BT_ClassFileException( "duplicate field definition " + name);
			}
			else {
				field.setFlags(modifiers);
			}
			field.setStub(false);
			if (DEBUG) System.out.println("found a field: "+field);
		}
		else
		if (token.equals("(")) {
			// we just read the beginning of a method declaration
			BT_MethodSignature sig = readMethodSignature(type);
			
			BT_Method method = cls.findMethodOrNull(name, sig);
			if(method == null) {
				method = BT_Method.createMethod(cls, modifiers, sig, name);
			} else if(!method.isStub()) {
				throw new BT_ClassFileException("duplicate method definition " + name + sig);
			} else {
				method.setFlags(modifiers);
			}
			readMethodBody(method);
			method.setStub(false);
		} else {
			throw new UnexpectedTokenException(scanner.lineNumber, "Expecting field or method declaration, not "+token);
		}
	}
	
	//TODO match these up to the opcode strings in BT_Misc, the throws and so on
	private void readMethodBody(BT_Method method) throws UnexpectedTokenException, BT_ClassFileException {
		token = nextToken();
		if (token.equals("throws")) {
			while (true) {
				token = nextToken();
				method.addDeclaredException(repository.forName(token));
				token = nextToken();
				if (token.equals("{") || token.equals(";"))
					break;
				expect(",");											
			}
		}
		if(token.equals(";")) {
			return;
		}
		expect("{");
		if (DEBUG) System.out.println("now parsing body for "+method);		
		BT_CodeAttribute code = new BT_CodeAttribute(new BT_Ins[0], method.getVersion());
		method.setCode(code);
		HashMap basicBlocks = new HashMap();
		token = nextToken();
		ArrayList rangesList = new ArrayList();
		while (true) {
			if(token == null) {
				throw new UnexpectedTokenException(scanner.lineNumber);
			}
			if(token.equals("}")) {
				break;
			}
			if (DEBUG) System.out.println("token="+token);
			
			
			if (token.equals("nop")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_nop));
			} else if (token.equals("aconst_null")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_aconst_null));
			} else if (token.equals("iconst_m1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iconst_m1));
			} else if (token.equals("iconst_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iconst_0));
			} else if (token.equals("iconst_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iconst_1));
			} else if (token.equals("iconst_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iconst_2));
			} else if (token.equals("iconst_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iconst_3));
			} else if (token.equals("iconst_4")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iconst_4));
			} else if (token.equals("iconst_5")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iconst_5));
			} else if (token.equals("lconst_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lconst_0));
			} else if (token.equals("lconst_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lconst_1));
			} else if (token.equals("fconst_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fconst_0));
			} else if (token.equals("fconst_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fconst_1));
			} else if (token.equals("fconst_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fconst_2));
			} else if (token.equals("dconst_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dconst_0));
			} else if (token.equals("dconst_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dconst_1));
			} else if (token.equals("bipush")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_bipush, readIntegerValue()));
			} else if (token.equals("sipush")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_sipush, readIntegerValue()));
			} else if (token.equals("ldc") || token.equals("ldc_w") || token.equals("ldc2_w")) {
				String value = nextToken();
				expect("(");
				String cast = nextToken();
				nextToken();
				expect(")");
				value = nextToken();
				if(cast.equals("java.lang.String")) {
					appendInstruction(code, rangesList, BT_Ins.make(opc_ldc, value, repository));
				} else if(cast.equals("java.lang.Class")) {
					appendInstruction(code, rangesList, BT_Ins.make(opc_ldc, repository.forName(BT_Repository.internalToStandardClassName(value))));
				} else if(cast.equals("int")) {
					appendInstruction(code, rangesList, BT_Ins.make(opc_ldc, Integer.parseInt(value)));
				} else if(cast.equals("long")) {
					appendInstruction(code, rangesList, BT_Ins.make(opc_ldc2_w, Long.parseLong(value)));
				} else if(cast.equals("float")) {
					appendInstruction(code, rangesList, BT_Ins.make(opc_ldc, Float.parseFloat(value)));
				} else if(cast.equals("double")) {
					appendInstruction(code, rangesList, BT_Ins.make(opc_ldc2_w, Double.parseDouble(value)));
				} else {
					throw new UnexpectedTokenException(scanner.lineNumber, "Unexpected argument to "+token+": "+cast);
				}
			} else if (token.equals("iload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iload, Integer.parseInt(nextToken())));
			} else if (token.equals("lload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lload, Integer.parseInt(nextToken())));
			} else if (token.equals("fload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fload, Integer.parseInt(nextToken())));
			} else if (token.equals("dload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dload, Integer.parseInt(nextToken())));
			} else if (token.equals("aload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_aload, Integer.parseInt(nextToken())));
			} else if (token.equals("iload_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iload_0));
			} else if (token.equals("iload_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iload_1));
			} else if (token.equals("iload_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iload_2));
			} else if (token.equals("iload_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iload_3));
			} else if (token.equals("lload_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lload_0));
			} else if (token.equals("lload_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lload_1));
			} else if (token.equals("lload_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lload_2));
			} else if (token.equals("lload_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lload_3));
			} else if (token.equals("fload_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fload_0));
			} else if (token.equals("fload_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fload_1));
			} else if (token.equals("fload_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fload_2));
			} else if (token.equals("fload_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fload_3));
			} else if (token.equals("dload_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dload_0));
			} else if (token.equals("dload_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dload_1));
			} else if (token.equals("dload_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dload_2));
			} else if (token.equals("dload_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dload_3));
			} else if (token.equals("aload_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_aload_0));
			} else if (token.equals("aload_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_aload_1));
			} else if (token.equals("aload_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_aload_2));
			} else if (token.equals("aload_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_aload_3));
			} else if (token.equals("iaload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iaload));
			} else if (token.equals("laload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_laload));
			} else if (token.equals("faload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_faload));
			} else if (token.equals("daload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_daload));
			} else if (token.equals("aaload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_aaload));
			} else if (token.equals("baload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_baload));
			} else if (token.equals("caload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_caload));
			} else if (token.equals("saload")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_saload));
			} else if (token.equals("istore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_istore, Integer.parseInt(nextToken())));
			} else if (token.equals("lstore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lstore, Integer.parseInt(nextToken())));
			} else if (token.equals("fstore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fstore, Integer.parseInt(nextToken())));
			} else if (token.equals("dstore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dstore, Integer.parseInt(nextToken())));
			} else if (token.equals("astore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_astore, Integer.parseInt(nextToken())));
			} else if (token.equals("istore_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_istore_0));
			} else if (token.equals("istore_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_istore_1));
			} else if (token.equals("istore_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_istore_2));
			} else if (token.equals("istore_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_istore_3));
			} else if (token.equals("lstore_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lstore_0));
			} else if (token.equals("lstore_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lstore_1));
			} else if (token.equals("lstore_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lstore_2));
			} else if (token.equals("lstore_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lstore_3));
			} else if (token.equals("fstore_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fstore_0));
			} else if (token.equals("fstore_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fstore_1));
			} else if (token.equals("fstore_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fstore_2));
			} else if (token.equals("fstore_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fstore_3));
			} else if (token.equals("dstore_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dstore_0));
			} else if (token.equals("dstore_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dstore_1));
			} else if (token.equals("dstore_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dstore_2));
			} else if (token.equals("dstore_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dstore_3));
			} else if (token.equals("astore_0")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_astore_0));
			} else if (token.equals("astore_1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_astore_1));
			} else if (token.equals("astore_2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_astore_2));
			} else if (token.equals("astore_3")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_astore_3));
			} else if (token.equals("iastore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iastore));
			} else if (token.equals("lastore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lastore));
			} else if (token.equals("fastore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fastore));
			} else if (token.equals("dastore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dastore));
			} else if (token.equals("aastore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_aastore));
			} else if (token.equals("bastore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_bastore));
			} else if (token.equals("castore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_castore));
			} else if (token.equals("sastore")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_sastore));
			} else if (token.equals("pop")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_pop));
			} else if (token.equals("pop2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_pop2));
			} else if (token.equals("dup")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dup));
			} else if (token.equals("dup_x1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dup_x1));
			} else if (token.equals("dup_x2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dup_x2));
			} else if (token.equals("dup2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dup2));
			} else if (token.equals("dup2_x1")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dup2_x1));
			} else if (token.equals("dup2_x2")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dup2_x2));
			} else if (token.equals("swap")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_swap));
			} else if (token.equals("iadd")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iadd));
			} else if (token.equals("ladd")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ladd));
			} else if (token.equals("fadd")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fadd));
			} else if (token.equals("dadd")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dadd));
			} else if (token.equals("isub")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_isub));
			} else if (token.equals("lsub")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lsub));
			} else if (token.equals("fsub")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fsub));
			} else if (token.equals("dsub")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dsub));
			} else if (token.equals("imul")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_imul));
			} else if (token.equals("lmul")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lmul));
			} else if (token.equals("fmul")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fmul));
			} else if (token.equals("dmul")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dmul));
			} else if (token.equals("idiv")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_idiv));
			} else if (token.equals("ldiv")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ldiv));
			} else if (token.equals("fdiv")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fdiv));
			} else if (token.equals("ddiv")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ddiv));
			} else if (token.equals("irem")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_irem));
			} else if (token.equals("lrem")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lrem));
			} else if (token.equals("frem")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_frem));
			} else if (token.equals("drem")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_drem));
			} else if (token.equals("ineg")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ineg));
			} else if (token.equals("lneg")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lneg));
			} else if (token.equals("fneg")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fneg));
			} else if (token.equals("dneg")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dneg));
			} else if (token.equals("ishl")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ishl));
			} else if (token.equals("lshl")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lshl));
			} else if (token.equals("ishr")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ishr));
			} else if (token.equals("lshr")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lshr));
			} else if (token.equals("iushr")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iushr));
			} else if (token.equals("lushr")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lushr));
			} else if (token.equals("iand")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iand));
			} else if (token.equals("land")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_land));
			} else if (token.equals("ior")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ior));
			} else if (token.equals("lor")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lor));
			} else if (token.equals("ixor")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ixor));
			} else if (token.equals("lxor")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lxor));
			} else if (token.equals("iinc")) {
				int localNr = Integer.parseInt(nextToken());
				short increment = Short.parseShort(nextToken());
				appendInstruction(code, rangesList, BT_Ins.make(opc_iinc, localNr, increment));
			} else if (token.equals("i2l")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_i2l));
			} else if (token.equals("i2f")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_i2f));
			} else if (token.equals("i2d")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_i2d));
			} else if (token.equals("l2i")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_l2i));
			} else if (token.equals("l2f")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_l2f));
			} else if (token.equals("l2d")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_l2d));
			} else if (token.equals("f2i")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_f2i));
			} else if (token.equals("f2l")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_f2l));
			} else if (token.equals("f2d")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_f2d));
			} else if (token.equals("d2i")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_d2i));
			} else if (token.equals("d2l")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_d2l));
			} else if (token.equals("d2f")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_d2f));
			} else if (token.equals("int2byte")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_int2byte));
			} else if (token.equals("int2char")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_int2char));
			} else if (token.equals("int2short")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_int2short));
			} else if (token.equals("lcmp")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lcmp));
			} else if (token.equals("fcmpl")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fcmpl));
			} else if (token.equals("fcmpg")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_fcmpg));
			} else if (token.equals("dcmpl")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dcmpl));
			} else if (token.equals("dcmpg")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dcmpg));
			} else if (token.equals("ifeq")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ifeq, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("ifne")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ifne, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("iflt")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_iflt, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("ifge")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ifge, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("ifgt")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ifgt, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("ifle")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ifle, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("if_icmpeq")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_if_icmpeq, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("if_icmpne")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_if_icmpne, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("if_icmplt")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_if_icmplt, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("if_icmpge")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_if_icmpge, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("if_icmpgt")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_if_icmpgt, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("if_icmple")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_if_icmple, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("if_acmpeq")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_if_acmpeq, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("if_acmpne")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_if_acmpne, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("goto")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_goto, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("jsr")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_jsr, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("ret")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ret, Short.parseShort(nextToken())));
			} else if (token.equals("tableswitch")) {
				BT_BasicBlockMarkerIns defaultIns = findBasicBlockMarker(basicBlocks,nextToken());
				String token = nextToken();
				if (!token.equals("{")) {
					throw new UnexpectedTokenException(scanner.lineNumber, "Incomplete tableswitch, expecting to see {, and not "+token);
				}
				int low = Integer.parseInt(nextToken());
				int high = Integer.parseInt(nextToken());
				Vector targets = new Vector();
				while (true) {
					String target = nextToken();
					if (target.equals("}")) 
						break;
					targets.add(findBasicBlockMarker(basicBlocks,target));
				}
				BT_BasicBlockMarkerIns tableTargets[] = new BT_BasicBlockMarkerIns[targets.size()];
				targets.copyInto(tableTargets);
				appendInstruction(code, rangesList, new BT_TableSwitchIns(opc_tableswitch, low, high, defaultIns, tableTargets));				
			} else if (token.equals("lookupswitch")) {
				BT_BasicBlockMarkerIns defaultIns = findBasicBlockMarker(basicBlocks,nextToken());
				String token = nextToken();
				if (!token.equals("{")) {
					throw new UnexpectedTokenException(scanner.lineNumber, "Incomplete lookupswitch, expecting to see {, and not "+token);
				}
				Vector values = new Vector();
				Vector targets = new Vector();
				while (true) {
					String value = nextToken();
					if (value.equals("}"))
						break;
					values.addElement(value);
					String target = nextToken();
					if (target.equals("}")) {
						throw new UnexpectedTokenException(scanner.lineNumber, "Incomplete lookupswitch, expecting to see a label, and not "+token);
					}
					targets.add(findBasicBlockMarker(basicBlocks,target));
				}
				int lookupValues[] = new int[values.size()];
				for (int n=0; n<values.size(); n++) {
					lookupValues[n] = Integer.parseInt((String)values.elementAt(n));
				}
				BT_BasicBlockMarkerIns lookupTargets[] = new BT_BasicBlockMarkerIns[targets.size()];
				targets.copyInto(lookupTargets);
				appendInstruction(code, rangesList, new BT_LookupSwitchIns(opc_lookupswitch, defaultIns, lookupValues, lookupTargets));				
			} else if (token.equals("ireturn")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ireturn));
			} else if (token.equals("lreturn")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_lreturn));
			} else if (token.equals("freturn")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_freturn));
			} else if (token.equals("dreturn")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_dreturn));
			} else if (token.equals("areturn")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_areturn));
			} else if (token.equals("return")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_return));
			} else if (token.equals("getstatic")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_getstatic, findField()));
			} else if (token.equals("putstatic")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_putstatic, findField()));
			} else if (token.equals("getfield")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_getfield, findField()));
			} else if (token.equals("putfield")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_putfield, findField()));
			} else if (token.equals("invokevirtual")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_invokevirtual, findMethod()));
			} else if (token.equals("invokespecial")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_invokespecial, findMethod()));
			} else if (token.equals("invokestatic")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_invokestatic, findMethod()));
			} else if (token.equals("invokeinterface")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_invokeinterface, findMethod()));
			} else if (token.equals("new")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_new, findClass()));
			} else if (token.equals("newarray")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_newarray, findClass()));
			} else if (token.equals("anewarray")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_anewarray, findClass()));
			} else if (token.equals("arraylength")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_arraylength));
			} else if (token.equals("athrow")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_athrow));
			} else if (token.equals("checkcast")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_checkcast, findClass()));
			} else if (token.equals("instanceof")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_instanceof, findClass()));
			} else if (token.equals("monitorenter")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_monitorenter));
			} else if (token.equals("monitorexit")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_monitorexit));
			} else if (token.equals("wide")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_wide));
			} else if (token.equals("multianewarray")) {
				BT_Class cls = repository.forName(getClassName());
				appendInstruction(code, rangesList, BT_Ins.make(opc_multianewarray, cls, Short.parseShort(nextToken())));
			} else if (token.equals("ifnull")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ifnull, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("ifnonnull")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_ifnonnull, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("goto_w")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_goto_w, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("jsr_w")) {
				appendInstruction(code, rangesList, BT_Ins.make(opc_jsr_w, findBasicBlockMarker(basicBlocks,nextToken())));
			} else if (token.equals("exception")) {
				readExceptionDeclaration(basicBlocks, code);
			} else {
				String label = token;
				if (nextToken().equals(":")) {
					appendInstruction(code, rangesList, findBasicBlockMarker(basicBlocks, label));
				} else {
					throw new UnexpectedTokenException(scanner.lineNumber, "Incorrect instruction syntax: " + token);
				}
			}
			token = nextToken();
		}
		
		PcRange ranges[] = (PcRange[]) rangesList.toArray(new PcRange[rangesList.size()]);
		BT_LineNumberAttribute lineAttr = new BT_LineNumberAttribute(ranges, code);		
		BT_AttributeVector attributes = code.attributes;
		attributes.addElement(lineAttr);
		//code.dereferenceElements();
		code.dereference(method);
	}
	
//	Here is an example BT_LineNumberTableAttribute:
//	Each element depicts a series of instructions and the line number in the code for those instructions
//	com.ibm.ive.tools.japt.commandLine.Main.<init>
	//
//	pcRanges= BT_LineNumberAttribute$PcRange[6]  (id=178)
//		[0]= BT_LineNumberAttribute$PcRange  (id=179)
//			lineNumber= 30
//			startIns= null
//			startPC= 0
//		[1]= BT_LineNumberAttribute$PcRange  (id=180)
//			lineNumber= 26
//			startIns= null
//			startPC= 4
//		[2]= BT_LineNumberAttribute$PcRange  (id=181)
//			lineNumber= 27
//			startIns= null
//			startPC= 10
//		[3]= BT_LineNumberAttribute$PcRange  (id=182)
//			lineNumber= 28
//			startIns= null
//			startPC= 16
//		[4]= BT_LineNumberAttribute$PcRange  (id=183)
//			lineNumber= 31
//			startIns= null
//			startPC= 28
//		[5]= BT_LineNumberAttribute$PcRange  (id=184)
//			lineNumber= 32
//			startIns= null
//			startPC= 32
//			

	
	int previousLineNumber = -1;
	
	void appendInstruction(BT_CodeAttribute code, List rangesList, BT_Ins instruction) {
		code.getInstructions().addElement(instruction);
		if(scanner.lineNumber != previousLineNumber && !instruction.isBlockMarker()) {
			//each time we switch lines, and we have added a new instruction, then we need a new element
			int lineNum = previousLineNumber = scanner.lineNumber;
			PcRange programCounterRange = new DereferencedPCRange(instruction, lineNum);
			//programCounterRange.startPC = code.ins.indexOf(instruction); not correct, need byte index, not instruction index
			rangesList.add(programCounterRange);
		//the line numbers are being overcounted
		}
	}
	

	// general format:  		
	//
	//	    exception from label0 to label1 catch java.lang.Exception handler label3					
	//
	private void readExceptionDeclaration(HashMap basicBlocks, BT_CodeAttribute code) throws UnexpectedTokenException {
		if (DEBUG) System.out.println("current labels: "+basicBlocks);
		expect(BT_ExceptionTableEntry.EXCEPTION);
		nextToken();
		expect(BT_ExceptionTableEntry.FROM);
		BT_BasicBlockMarkerIns label1 = (BT_BasicBlockMarkerIns)basicBlocks.get(BT_Ins.make(nextToken()));
		if (label1 == null)
			throw new UnexpectedTokenException(scanner.lineNumber, "Invalid label: "+token);
		nextToken();
		expect(BT_ExceptionTableEntry.TO);
		BT_BasicBlockMarkerIns label2 = (BT_BasicBlockMarkerIns)basicBlocks.get(BT_Ins.make(nextToken()));
		if (label2 == null)
			throw new UnexpectedTokenException(scanner.lineNumber, "Invalid label: "+token);
		nextToken();
		expect(BT_ExceptionTableEntry.CATCH);
		BT_Class type = null;
		String classNameString = nextToken();
		if (classNameString.equals("(")) {
			nextToken();
			expect(BT_ExceptionTableEntry.ANY);
			nextToken();
			expect(")");
			type = null;
		} else { 
			type = repository.forName(classNameString);
		}
		nextToken();
		expect(BT_ExceptionTableEntry.HANDLER);
		BT_BasicBlockMarkerIns label3 = (BT_BasicBlockMarkerIns)basicBlocks.get(BT_Ins.make(nextToken()));
		if (label3 == null)
			throw new UnexpectedTokenException(scanner.lineNumber, "Invalid label: " + token);
		
		BT_ExceptionTableEntry e = new BT_ExceptionTableEntry(label1, label2, label3, type, code);
		code.insertExceptionTableEntry(e);
	}

	private BT_Class findClass() throws UnexpectedTokenException {
		return repository.forName(getClassName());
	}
	
	private String getClassName() throws UnexpectedTokenException {
		String name = nextToken();
		nextToken();
		name = appendArrayDepth(name);
		putBackToken();
		return name;
	}
	
	private BT_Field findField() throws UnexpectedTokenException {
		String name = nextToken();
		if (DEBUG) System.out.println("name="+name);
		
		String type = nextToken();
		nextToken();
		type = appendArrayDepth(type);
		putBackToken();
		if (DEBUG) System.out.println("type="+type);
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex == -1) 
			throw new UnexpectedTokenException(scanner.lineNumber, "Incomplete field, expecting to see field name, and not "+token);
		String className = name.substring(0, dotIndex);
		String fieldName = name.substring(dotIndex+1);
		BT_Class clazz = repository.forName(className);
		BT_Class typeClass = repository.forName(type);
		try {
			return clazz.findField(fieldName, typeClass);
		} catch (BT_NoSuchFieldException e) {
			BT_Field field = BT_Field.createField(clazz, BT_Class.PUBLIC, typeClass, fieldName);
			field.setStub(true);
			if (DEBUG) System.out.println("create stub Field: "+type+" "+fieldName+ "->"+field);
			return field;
		}
	}

	private BT_BasicBlockMarkerIns findBasicBlockMarker(HashMap basicBlocks, String token) {
		BT_BasicBlockMarkerIns target = BT_Ins.make(token);
		BT_BasicBlockMarkerIns bbm = (BT_BasicBlockMarkerIns)basicBlocks.get(target);
		if (bbm == null) {
			bbm = target;
			if (DEBUG) System.out.println("adding basic block '"+token+"'");			
			basicBlocks.put(bbm, bbm);
		}
		return bbm;
	}

	private BT_Method findMethod() throws UnexpectedTokenException {
		String returnType = nextToken();
		
		nextToken();
		returnType = appendArrayDepth(returnType);
		if (DEBUG) System.out.println("returnType = "+returnType);
		//String name = nextToken();
		String name = token;
		if (DEBUG) System.out.println("name = "+name);
		token = nextToken();
		expect("(");
		String string = readMethodSignatureLastPart(name);
		int braceIndex = string.lastIndexOf('(');
		if (braceIndex == -1) 
			throw new UnexpectedTokenException(scanner.lineNumber, "Incomplete method, expecting to see signature "+token);
		int dotIndex = string.lastIndexOf('.', braceIndex);
		if (dotIndex == -1) 
			throw new UnexpectedTokenException(scanner.lineNumber, "Incomplete method, expecting to see a method name and not "+token);
		String className = string.substring(0, dotIndex);
		String methodName = string.substring(dotIndex+1,braceIndex);
		String sig = string.substring(braceIndex);
		BT_Class clazz = repository.forName(className);
		try {
			return clazz.findMethod(methodName, sig);
		} catch (BT_NoSuchMethodException e) {
			BT_MethodSignature signature = BT_MethodSignature.create(returnType, sig, clazz.getRepository());
			BT_Method method = BT_Method.createMethod(clazz, BT_Method.PUBLIC, signature, methodName);
			method.setStub(true);
			return method;
		}
	}

	private String readMethodSignatureLastPart(String name) throws UnexpectedTokenException {
		StringBuffer method = new StringBuffer(name);
		method.append("(");
		while (true) {
			if (token.equals(")")) 
				break;
			if (token.equals(",") || isIdentifier(token) || isArrayBracket(token))
				method.append(token); 
			else
			if (!token.equals("("))
				throw new UnexpectedTokenException(scanner.lineNumber, "Incomplete method signature, did not expect "+token);
			token = nextToken();
		}
		method.append(")");
		return method.toString();
	}


	private BT_MethodSignature readMethodSignature(String returnType) throws UnexpectedTokenException {
		return BT_MethodSignature.create(returnType, readMethodSignatureLastPart(""), repository);
	}




	
	/////////////////////////////////////////////////////////////////////////
	//   Generic parse methods
	/////////////////////////////////////////////////////////////////////////

	private int readIntegerValue() throws UnexpectedTokenException {
		String s = nextToken();
		try {
			return Integer.parseInt(s);
		}
		catch (Exception e) {
			throw new UnexpectedTokenException(scanner.lineNumber, "Expecting Integer value, not "+s);
		}
	}

	private Vector readIdentifierList() {
		Vector result = new Vector();
		while (true) {
			result.add(token);
			nextToken();
			if (!token.equals(",")) 	
				break;
			nextToken();
		}
		return result;
	}
	
	private void expect(String string) throws UnexpectedTokenException {
		if (!token.equals(string))
			throw new UnexpectedTokenException(scanner.lineNumber, "Expected: "+string+", found:" +token);
	}
	
	private void expect(String strings[]) throws UnexpectedTokenException {
		StringBuffer total = new StringBuffer(strings.length * 10);
		for(int i=0; i<strings.length; i++) {
			if (!token.equals(strings[i])) {
				return;
			}
			total.append(strings[i]);
			total.append(' ');
		}
		throw new UnexpectedTokenException(scanner.lineNumber, "Expected: " + total + ", found:" +token);
	}

	String nextToken() {
		if(storedToken != null) {
			token = storedToken;
			storedToken = null;
		}
		else {
			token = scanner.getToken();
		}
		return token;
	}
	
	void putBackToken() {
		if(storedToken != null) {
			throw new RuntimeException();
		}
		storedToken = token;
	}
	
	private boolean isIdentifier(String token) {
		return token.length()>0 && Character.isJavaIdentifierStart(token.charAt(0));
	}

	private boolean isArrayBracket(String token) {
		return token.length()==1 && (token.charAt(0)=='[' || token.charAt(0)==']');
	}


}
