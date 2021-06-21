package helpers;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *	file reader helper
 */
public class FileReader {
	// file name
	private String fileName;
	// record data value
	private ArrayList<String> dataList;
	// iterator for line
	private Iterator<String> it;
	
	/**
	 * file reader
	 * @param fileName file name
	 * @throws IOException
	 */
	public FileReader(String fileName) throws IOException {
		this.fileName = fileName;
		this.dataList = dataListGenerator();
		this.it = dataList.iterator();
	}

	/**
	 * record file data in the list
	 * @return list of file data
	 * @throws IOException
	 */
	public ArrayList<String> dataListGenerator() throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
        String line = reader.readLine();
        ArrayList<String> dataList = new ArrayList<>();
        while (line != null) {
        	dataList.add(line);
        	line = reader.readLine();
        }
        reader.close();
		return dataList;
	}
	
	/**
	 * let reader read the file infinitely
	 * @return a line in the file
	 * @throws IOException
	 */
	public String getInfiniteDataRow() throws IOException {
		if (it.hasNext()) {
			return it.next();
		}
		it = dataList.iterator();
		return it.next();		
	}
}
