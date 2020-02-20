package org.statnlp.example.tagging;

import java.util.HashMap;

public class WordDict {

	public WordDict() {
		locked = false;
	}
	
	HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
	
	boolean locked = false;
	
	public boolean useLowerCase = false;
	
	public void setLock() {
		setLock(true);
	}
	
	public void setLock(boolean lock) 
	{
		this.locked = lock;
	}


	
	public int addWord(String word) {
		
		if (useLowerCase)
			word = word.toLowerCase();
		
		Integer v = wordCount.get(word);
		
		if (v == null) {
			v = 0;
		}
		
		if (!locked) {
			v++;
			wordCount.put(word, v);
		}
		
		return v;
	}
	
	public int getWordCount(String word) {
		if (useLowerCase)
			word = word.toLowerCase();
		Integer v = wordCount.get(word);
		if (v == null) {
			v = 0;
		}
		return v;
	}

	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("Dict:\n");
		sb.append(wordCount.toString());
		return sb.toString();
	}
}
