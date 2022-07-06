<<<<<<<< HEAD:src/fr/verveine/utils/IncludeConfs.java
package fr.verveine.utils;
========
package verveine.extractor.Cpp;
>>>>>>>> master:test/verveine/extractor/Cpp/IncludeConfs.java

import java.io.IOException;
import java.io.InputStreamReader;

public class IncludeConfs {

	public static void main(String[] args) {

        try {
        	/*
        	 * trying to automatically configure include path by running cpp -v
        	 */
            Process p = Runtime.getRuntime().exec("cpp -v");
            
            InputStreamReader stdInput = new //BufferedReader(new 
                 InputStreamReader(p.getInputStream()) ; //);

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while (stdInput.ready() ) {
                System.out.print(stdInput.read());
            }
        }
        catch (IOException e) {
        	System.out.println("exception happened - here's what I know: ");
        	e.printStackTrace();
        	System.exit(-1);
        }
	}

}
