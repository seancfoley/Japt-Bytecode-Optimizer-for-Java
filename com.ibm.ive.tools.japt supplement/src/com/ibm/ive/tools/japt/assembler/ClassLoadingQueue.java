/*
 * Created on Oct 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.assembler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassFileException;
import com.ibm.jikesbt.BT_ClassVersion;
import com.ibm.jikesbt.BT_DuplicateClassException;
import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ClassLoadingQueue {

	private ArrayList toLoad = new ArrayList();
	private JaptRepository repository;
	private boolean loading;
	private AssemblerMessages messages;
	private Logger logger;
	private final BT_ClassVersion classVersion;
	
	class ToLoad {
		String className;
		BT_ClassPathLocation loc;
		BT_Class stub;
		
		ToLoad(String s, BT_ClassPathLocation l, BT_Class stub) {
			this.className = s;
			this.loc = l;
			this.stub = stub;
		}
		
		public boolean equals(Object o) {
			if(o instanceof ToLoad) {
				return className.equals(((ToLoad) o).className);
			}
			return false;
		}
	}
	
	/**
	 * 
	 */
	public ClassLoadingQueue(
		JaptRepository repository,
		BT_ClassVersion classVersion, 
		AssemblerMessages messages,
		Logger logger) {
		this.repository = repository;
		this.messages = messages;
		this.logger = logger;
		this.classVersion = classVersion;
	}
	
	public void add(String className, BT_ClassPathLocation loc, BT_Class stub) {
		ToLoad load = new ToLoad(className, loc, stub);
		if(!toLoad.contains(load)) {
			toLoad.add(load);
		}
	}
	
	public void clear() {
		toLoad.clear();
	}
	
	protected BT_Class loadClass(String className, BT_ClassPathLocation loc, BT_Class stub) {
		if(loading) {
			//TODO sean this here and loadAssemblyClass below will need the same locking as in BT_Repository/BT_Class xxx;
			if(stub == null) {
				stub = repository.createStub(className); //TODO sean must grab table lock when calling this xxx;
			}
			//TODO sean while creating the class structure, need to hold the class lock
			
			add(className, loc, stub); //add to the queue to be loaded later
			return stub;
		}
		loading = true;
		try {
			stub = loadAssemblyClass(className, loc, stub);
			doQueuedLoads(); /* stubs referenced are never loaded if this load fails */
		} finally {
			loading = false;
			clear();
		}
		return stub;
	}
	
	/**
	 * @param className
	 * @param repository
	 * @param loc
	 * @return
	 */
	private BT_Class loadAssemblyClass(String className, BT_ClassPathLocation loc, BT_Class stub) {
		BT_Class clazz;
		try {
			clazz = loadAssemblyClass(loc, stub);
		}
		catch(BT_ClassFileException e) {
			messages.CLASS_ERROR.log(logger, new Object[] {loc.getName(), e});
			clazz = repository.createStub(className); //TODO sean must grab table lock when calling this xxx;
		}
		catch(InvalidIdentifierException e) {
			messages.CLASS_ERROR.log(logger, new Object[] {loc.getName(), e});
			clazz = repository.createStub(className); //TODO sean must grab table lock when calling this xxx;
		}
		catch(BT_DuplicateClassException e) {
			messages.DUP_CLASS.log(logger, loc.getName());
			clazz = e.getOld();
		}
		catch(IOException e) {
			messages.ERROR_SOURCE.log(logger, loc.getName());
			clazz = repository.createStub(className);
		}
		catch(UnexpectedTokenException e) {
			messages.ERROR_SOURCE_LINE.log(logger, new Object[] {Integer.toString(e.line), loc.getName()});
			clazz = repository.createStub(className);
		}
		//repository.registerClass(className, (ClassPathEntry) loc.getClassPathEntry(), clazz);
		return clazz;
	}
	
	/**
	 * @param className
	 * @param repository
	 * @param loc
	 * @return
	 */
	private BT_Class loadAssemblyClass(BT_ClassPathLocation loc, BT_Class stub) 
		throws IOException, 
			UnexpectedTokenException, 
			BT_DuplicateClassException, 
			BT_ClassFileException, 
			InvalidIdentifierException {
		Scanner scanner = null;
		InputStream is = loc.getInputStream();
		BT_Class clazz;
        try {
			byte buffer[] = new byte[is.available()];
	        int bytes = 0, totalBytes = 0;
	        while(bytes >= 0 && totalBytes < buffer.length) {
	        	bytes = is.read(buffer, totalBytes, buffer.length - totalBytes);
	        	totalBytes += bytes;
	        }
	        scanner = new Scanner(buffer, loc.getName());
	        Parser p = new Parser(repository, (ClassPathEntry) loc.getClassPathEntry(), scanner, classVersion);
	        clazz = p.parse(stub);
		} finally {
			is.close();
		}
		return clazz;
	}
	
	protected void doQueuedLoads() {
		/* note that toLoad.size() size may increase as we iterate through the loop */
		for(int i=0; i<toLoad.size(); i++) {
			ToLoad related = (ToLoad) toLoad.get(i);
			loadAssemblyClass(related.className, related.loc, related.stub);
		}
	}
}
