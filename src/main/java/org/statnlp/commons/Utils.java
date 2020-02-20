/**
 * 
 */
package org.statnlp.commons;

import java.io.PrintStream;

/**
 * Collection of static methods
 */
public class Utils {
	
	public static boolean DEBUG = false;

	public static void print(String string, PrintStream... streams){
		if (DEBUG) return;
		if(streams == null || streams.length == 0){
			streams = new PrintStream[]{System.out};
		}
		for(PrintStream stream: streams){
			if(stream == null){
				continue;
			}
			stream.println(string);
		}
	}
}
