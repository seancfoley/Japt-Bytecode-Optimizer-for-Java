/*
 * Created on Sep 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.assembler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.commandLine.options.IdentifierOption;
import com.ibm.ive.tools.japt.out.DirGenerator;
import com.ibm.ive.tools.japt.out.JarGenerator;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeException;


/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DisassemblerExtension extends BaseAssemblerExtension {

	private DisassemblerMessages messages = new DisassemblerMessages(this);
	private String name = messages.DESCRIPTION;
	
	private IdentifierOption disassemble = new IdentifierOption(messages.DISASSEMBLE_LABEL, messages.DISASSEMBLE);
	private ValueOption disAssemblyTarget = new ValueOption(messages.TARGET_LABEL, messages.TARGET); 
	//TODO a source file class path
	
	/**
	 * 
	 */
	public DisassemblerExtension() {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		
		String fileExtension = getFileExtension();
		
		if(disassemble.appears()) {
			Identifier classIdentifiers[] = disassemble.getIdentifiers();
			BT_ClassVector allClasses = null;
			for(int i=0; i<classIdentifiers.length; i++) {
				Identifier ci = classIdentifiers[i];
				try {
					BT_ClassVector classes = repository.findClasses(ci, true);
					if(allClasses == null) {
						allClasses = classes;
					}
					else {
						allClasses.addAll(classes);
					}
				}
				catch(InvalidIdentifierException e) {
					repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
				}
			}
			if(allClasses.size() > 0 && disAssemblyTarget.appears()) {
				File outputTarget = new File(disAssemblyTarget.getValue());
				if(!outputTarget.isDirectory()) {
					try {
						JarGenerator defaultJar = new JarGenerator(new FileOutputStream(outputTarget));
						for(int i=0; i<allClasses.size(); i++) {
							BT_Class clazz = allClasses.elementAt(i);
							try {
								defaultJar.writeAssemblyClass(clazz, fileExtension);
							} catch(BT_CodeException e) {
								repository.getFactory().noteCodeException(e);
							}
						}
						defaultJar.close();
						messages.CREATED_JAR.log(logger, outputTarget);
					}
					catch(IOException e) {
						messages.ERROR_WRITING_JAR.log(logger);
					}
				}
				else {
					DirGenerator dirGenerator = 
						new DirGenerator(outputTarget, new com.ibm.ive.tools.japt.out.Messages(this), logger);
					BT_Class clazz = null;
					try {
						for(int i=0; i<allClasses.size(); i++) {
							clazz = allClasses.elementAt(i);
							try {
								dirGenerator.writeAssemblyClass(clazz, fileExtension);
							} catch(BT_CodeException e) {
								repository.getFactory().noteCodeException(e);
							}
						}
					}
					catch(IOException e) {
						messages.ERROR_WRITING.log(logger, clazz.getName());
					}
				}
			}
		}
		
	}
	
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return null or an array of options pertaining to the extension
	 */
	public Option[] getOptions() {
		return combine(new Option[] { 
				disassemble, 
				disAssemblyTarget
		}, super.getOptions());
	}
}
