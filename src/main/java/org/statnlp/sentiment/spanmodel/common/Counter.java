package org.statnlp.sentiment.spanmodel.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Counter {

	HashMap<String, HashMap<String, Integer>> counter = new HashMap<String, HashMap<String, Integer>>();
	int N = 5;
	// HashMap<String, Integer> counterPerClass = new HashMap<String,
	// Integer>();

	public Counter() {
		// TODO Auto-generated constructor stub
	}
	
	public Counter(int N) {
		this.setN(N);
	}
	
	public void setN(int N)
	{
		this.N = N;
	}
	
	public void clear()
	{
		this.counter.clear();
	}

	public void addWordForClass(String word, String Class) {
		
		word = word.trim();
		
		if (word.length() == 0)
			return;
		
		HashMap<String, Integer> counterPerClass = counter.get(Class);

		if (counterPerClass == null) {
			counterPerClass = new HashMap<String, Integer>();
			counter.put(Class, counterPerClass);
		}

		Integer count = counterPerClass.get(word);

		if (count == null) {
			count = 0;
		}
		counterPerClass.put(word, count + 1);

	}

	public String[] topNWordForClass(String Class, int N) {
		String[] topN = new String[N];

		Map<String, Integer> counterPerClass = counter.get(Class);

		counterPerClass = sortByValue(counterPerClass);

		int i = 0;
		for (Map.Entry<String, Integer> entry : counterPerClass.entrySet()) {
			if (i >= N)
				break;

			topN[i] = entry.getKey() + "-" + entry.getValue();
			i++;
		}

		return topN;
	}
	
	public String[] getClasses()
	{
		String[] classes = new String[counter.keySet().size()];
		return counter.keySet().toArray(classes);
	}
	
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		String[] classes = this.getClasses();
		for(int i = 0; i < classes.length; i++)
		{
			String Class = classes[i];
			String[] topNWords = this.topNWordForClass(Class, N);
			sb.append(Class + "\t");
			for(int j = 0; j < topNWords.length; j++)
			{
				sb.append(topNWords[j] + " ");
			}
			sb.append("\n");
		}
		
		return sb.toString();
		
	}
	
	public ArrayList<String> getHighFrequencyWords(String Class)
	{
		ArrayList<String> highFreqWords = new ArrayList<String>();
		
		Map<String, Integer> counterPerClass = counter.get(Class);

		counterPerClass = sortByValue(counterPerClass);

		int i = 0;
		for (Map.Entry<String, Integer> entry : counterPerClass.entrySet()) {
			if (i >= N)
				break;

			highFreqWords.add(entry.getKey());
			i++;
		}
		
		return highFreqWords;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return -(o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

}
