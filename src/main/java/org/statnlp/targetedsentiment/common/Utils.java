package org.statnlp.targetedsentiment.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

public class Utils {

	public static TSInstance[] portionInstances(TSInstance[] instances, double percentage) {
		return portionInstances(instances, (int)(percentage * instances.length));
	}
	
	public static TSInstance[] portionInstances(TSInstance[] instances, int num) {
		//TSInstance[] insts = new TSInstance[num];
		if (num > instances.length)
			num = instances.length;
		System.out.println("Truncate " + num + " instances.");
		return Arrays.copyOf(instances, num);
	}
	
	public static TSInstance[] mergeInstances(TSInstance[] instances1, TSInstance[] instances2) {
		TSInstance[] instances = new TSInstance[instances1.length + instances2.length];
		for(int i = 0; i < instances1.length; i++)
			instances[i] = instances1[i];
		
		for(int i = 0; i < instances2.length; i++)
			instances[instances1.length + i] = instances2[i];
		
		return instances;
	}
	
	public static boolean isPunctuation(char c) {
        return c == ','
            || c == '.'
            || c == '!'
            || c == '?'
            || c == ':'
            || c == ';'
            ;
    }
	
	public static boolean isPunctuation(String s) {
        return s.length() == 1 && isPunctuation(s.charAt(0));
    }
	
	public static void writeVocab(String filename, TSInstance[][] instancesList, boolean lowercase) {
		PrintWriter p = null;
		try {
			p = new PrintWriter(new File(filename), "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (TSInstance[] instances : instancesList)
			for (TSInstance inst : instances) {
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
				for (String[] token : input) {
					;
					String output = token[0];
					if (lowercase)
						output = output.toLowerCase();
					p.println(output);
				}
			}
		
		p.close();
	}
}
