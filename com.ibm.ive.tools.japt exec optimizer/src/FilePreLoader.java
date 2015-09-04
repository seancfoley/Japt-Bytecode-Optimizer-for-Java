import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class FilePreLoader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new FilePreLoader().preload();
	}
	
	void preload() {
		try {
			InputStream stream = getClass().getResourceAsStream("PreLoadClasses.txt");
			BufferedReader classesReader = new BufferedReader(new InputStreamReader(stream));
			while(true) {
				String line = classesReader.readLine();
				if(line == null) {
					break;
				}
				try {
					Class.forName(line);
				} catch(ClassNotFoundException e) {
					System.err.println("preloading failed: " + e);
				}
			} 
		} catch(FileNotFoundException e) {
			//no pre-loading requested
		} catch(IOException e) {
			System.err.println("preloading failed: " + e);
		}
	}
	

}
