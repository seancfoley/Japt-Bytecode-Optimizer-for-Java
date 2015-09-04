package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.SyntheticClassPathEntry;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;

public class ClassPathSplitter {
	public final JaptRepository repository;
	final Repository rep;
	static final ClassPathEntry emptyEntries[] = new ClassPathEntry[0];
	public static final String UNREACHABLE_SUFFIX = "-inaccessible";
	public static final String RT_SUFFIX = "-rt";
	public static final String NHRT_SUFFIX = "-nhrt";
	public static final String RESOURCE_SUFFIX = "-resources";
	public static final String JAVA_LANG_THREAD_SUFFIX = "-t";
	
	public ClassPathSplitter(
			JaptRepository repository,
			Repository rep) {
		this.rep = rep;
		this.repository = repository;
	}
	
	public static String[] splitName(String name) {
		int ext = name.lastIndexOf('.');
		if(ext < 0) {
			return new String[] {name, ".jar"};
		}
		return new String[] {name.substring(0, ext), name.substring(ext)};
	}
	
	public ClassPathEntry[] split(ClassPathEntry cpe) {
		BT_ClassVector clzs = (BT_ClassVector) cpe.getLoadedClasses().clone();
		String names[] = splitName(cpe.getName());
		SyntheticClassPathEntry unreachable = new SyntheticClassPathEntry(names[0] + UNREACHABLE_SUFFIX + names[1], cpe.isArchive());
		unreachable.setContextFlags(RTSJCallingContext.NOT_ACCESSED);
		SyntheticClassPathEntry rt = new SyntheticClassPathEntry(names[0] + RT_SUFFIX + names[1], cpe.isArchive());
		rt.setContextFlags(RTSJCallingContext.REAL_TIME_ACCESSED);
		SyntheticClassPathEntry nhrt = new SyntheticClassPathEntry(names[0] + NHRT_SUFFIX + names[1], cpe.isArchive());
		nhrt.setContextFlags(RTSJCallingContext.NO_HEAP_REAL_TIME_ACCESSED);
		SyntheticClassPathEntry t = new SyntheticClassPathEntry(names[0] + JAVA_LANG_THREAD_SUFFIX + names[1], cpe.isArchive());
		t.setContextFlags(RTSJCallingContext.JAVA_LANG_THREAD_ACCESSED);
		
		/* we transfer everything into the new class path entries.  Only the resources remain. */
		cpe.setName(names[0] + RESOURCE_SUFFIX + names[1]);
		for(int i=0; i<clzs.size(); i++) {
			BT_Class clz = clzs.elementAt(i);
			Clazz clazz = rep.contains(clz);
			if(clazz != null) {
				int flags = clazz.getContextFlags();
				if((flags & RTSJCallingContext.NO_HEAP_REAL_TIME_ACCESSED) == RTSJCallingContext.NO_HEAP_REAL_TIME_ACCESSED) {
					nhrt.addClass(clz);
					cpe.removeClass(clz);
				} else if((flags & RTSJCallingContext.REAL_TIME_ACCESSED) == RTSJCallingContext.REAL_TIME_ACCESSED) {
					rt.addClass(clz);
					cpe.removeClass(clz);
				} else {
					t.addClass(clz);
					cpe.removeClass(clz);
				}
			} else {
				unreachable.addClass(clz);
				cpe.removeClass(clz);
			}
		}
		
		
		boolean hasUnreachable = unreachable.hasLoaded();
		boolean hasNhrt = nhrt.hasLoaded();
		boolean hasRt = rt.hasLoaded();
		boolean hasReg = t.hasLoaded();
		int total = (hasUnreachable ? 1 : 0) + (hasNhrt ? 1 : 0) + (hasRt ? 1 : 0) + (hasReg ? 1 : 0);
		if(total == 0) {
			return emptyEntries;
		}
		ClassPathEntry res[] = new ClassPathEntry[total];
		int counter = 0;
		if(hasUnreachable) {
			res[counter++] = unreachable;
		}
		if(hasRt) {
			res[counter++] = rt;
		}
		if(hasReg) {
			res[counter++] = t;
		}
		if(hasNhrt) {
			res[counter] = nhrt;
		}
		return res;
	}
}
