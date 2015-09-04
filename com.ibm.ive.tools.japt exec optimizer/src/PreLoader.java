

public class PreLoader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean threw = false;
		for(int i=0; i<args.length; i++) {
			System.err.println("now loading " + args[i]);
			System.err.flush();
			try {
				Class.forName(args[i]);
			} catch(ClassNotFoundException e) {
				threw = true;
				System.err.println(e);
			}
		}
		if(!threw) {
			System.err.println("successfully preloaded all classes");
		}
	}

}
