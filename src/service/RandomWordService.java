package service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Random;

public class RandomWordService {


	public static String getNextWord() throws IOException {

		URL url = RandomWordService.class.getResource("/slowa.txt");
		System.out.println("file is: " + url.getPath());
		
		InputStream inputStream;
		InputStream inputStream2;

		int totalLines;
		String ran;
		ran = "";
		BufferedReader br;
		BufferedReader br2;
		try {
			totalLines = 0;
			inputStream = new FileInputStream(new File(url.getPath()));
			// InputStream input = new URL("http://www.somewebsite.com/a.txt").openStream();
			inputStream2 = new FileInputStream(new File(url.getPath()));
			br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

			while ((ran = br.readLine()) != null) {
				totalLines++;
				// System.out.println("wczytuje " + totalLines);
			}
			inputStream.close();
			br.close();
			// inputStream.close();
			br2 = new BufferedReader(new InputStreamReader(inputStream2, "UTF-8"));

			Random random = new Random();
			int randomInt = random.nextInt(totalLines);
			int count = 0;

			while (randomInt <= totalLines) {
				ran = br2.readLine();

				if (count == randomInt) {
					inputStream2.close();
					br2.close();
					
					break;

				}
				count++;

			}
			inputStream2.close();
			br2.close();
			return ran;

		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		} catch (IOException e) {
			System.out.println("Unable to read field ");
		}
		// System.out.println(ran);
		return "";

		/*
		 * Random random = new Random();
		 * 
		 * String[] tab = new String[] { "q", "w", "e", "r", "t", "y" };
		 * 
		 * String ran = tab[(random.nextInt(6))];
		 * 
		 * System.out.println(ran);
		 * 
		 * return ran;
		 * 
		 */

	}

}
