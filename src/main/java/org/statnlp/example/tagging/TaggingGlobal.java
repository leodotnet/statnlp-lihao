package org.statnlp.example.tagging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TaggingGlobal {

	public static boolean isTraining = true;
	
	public static WordDict dict = new WordDict();
	
	public static HashMap<String, Integer> tag2id = null;
	public static HashMap<Integer, String> id2tag = null;
	
	public static void resetDict()
	{
		dict = new WordDict();
		tag2id = new HashMap<String, Integer>();
		id2tag = new HashMap<Integer, String>();
	}
	
	public static void lockDict()
	{
		dict.setLock();
	}
	
	public static String UNK = "<UNK>";
	
	public static boolean filterLowFreqWords = false;
	
	public static boolean startOfEntityStr(int pos, int size, List<String> outputs)
	{
		String label = outputs.get(pos);
		if (label.startsWith("B"))
			return true;
		
		if (pos == 0 && label.startsWith("I"))
			return true;
		
		if (pos > 0)
		{
			String prev_label =  outputs.get(pos - 1);
			if (label.startsWith("I") && prev_label.startsWith("O"))
				return true;
			
			if (label.startsWith("I") && !prev_label.startsWith("O")) {
				if (!label.substring(2).equals(prev_label.substring(2))) {
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean endofEntityStr(int pos, int size,  List<String> outputs)
	{
		String label = outputs.get(pos);
		if (!label.startsWith("O"))
		{
			if (pos == size - 1)
				return true;
			else {
				String next_label =  outputs.get(pos + 1);
				if (next_label.startsWith("O") || next_label.startsWith("B"))
					return true;
				
				if (!label.startsWith("O") && next_label.startsWith("I")) {
					if (!label.substring(2).equals(next_label.substring(2))) {
						return true;
					}
				}
				
			}
		}
		
		return false;
	}

}
