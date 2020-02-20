package org.statnlp.targetedsentiment.common;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;



public class SentimentDict implements Serializable{
	
	
	public HashMap<String, Integer> word2polar = new HashMap<String, Integer>();
	
	public HashMap<String, Double> word2polar_double = new HashMap<String, Double>();
	
	public HashSet<String> negation = new HashSet<String>();
	
	public HashSet<String> intensity = new HashSet<String>();
	
	public static String SEPERATOR = "\t";

	public SentimentDict() {
		
	}
	
	public SentimentDict(String lexicons) {
		loadLexicons(lexicons);
	}
	
	
	public void loadLexicons(String lexicons)
	{
		String[] Lexicons = lexicons.split(",");
		for(int i = 0; i < Lexicons.length; i++)
		{
			String lexicon = Lexicons[i];
			
			switch(lexicon)
			{
			case "mpqa":this.loadMPQALexicon();break;
			case "sst":this.loadSSTLexicon(); break;
			case "opinion":this.loadOpinionLexicon();break;
			case "negation":this.loadLRSA(lexicon, negation);
			case "intensity":this.loadLRSA(lexicon, intensity);
			case "sent140":this.loadSent140();
			}
		}
	}
	
	public String queryWordType(String input) {
		
		String word = input.toLowerCase();
		Integer polarInt = word2polar.get(word);
		if (polarInt != null) {
			if (polarInt > 0) {
				return "positive";
			} else {
				return "negative";
			}
		} else {
			Double polarDouble = word2polar_double.get(word);
			if (polarDouble != null) {
				if (polarDouble > 0) {
					return "positive";
				} else {
					return "negative";
				}
			} else {
				if (this.negation.contains(word)) {
					return "negation";
				} else if (this.intensity.contains(word)) {
					return "intensity";
				} else if (word.equals("<pad>") || word.equals("<start>") || word.equals("<end>")) {
					return "<PAD>";
				} else {
					return "nonsentiment";
				}
			}
		}
	}
	
	public void loadLRSA(String lexiconName, HashSet<String> lexicon)
	{
		System.out.println("Loading LRSA " + lexiconName + " Lexicon...");
		String filename = "data//sentiment//lrsa//" + lexiconName + ".txt";
		InputStreamReader isr = null;
		lexicon.clear();
		try {
			isr = new InputStreamReader(new FileInputStream(filename), "UTF-8");
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader br = new BufferedReader(isr);
		
		try {
			while(br.ready()){
				
				String line = br.readLine().trim();
				if(line.startsWith("##") || line.length() == 0){
					continue;
				}
				
				String[] fields = line.split(",");
				
				for(String word : fields)
				{
					word = word.trim();
					if (word.length() == 0) continue;
					
					if (!lexicon.contains(word))
						lexicon.add(word);
				}
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void loadMPQALexicon()
	{
		System.out.println("Loading MPQA Lexicon...");
		String filename = "data//sentiment//mpqa//englishSubjLex-MPQA.tsv";
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(new FileInputStream(filename), "UTF-8");
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader br = new BufferedReader(isr);
		
		try {
			while(br.ready()){
				
				String line = br.readLine().trim();
				if(line.startsWith("##") || line.length() == 0){
					continue;
				}
				
				String[] fields = line.split(SEPERATOR);
				
				String word = fields[0];
				String Polar = fields[1];
				
				int polar = 0;
				if (Polar.equals("positive"))
				{
					polar = 1;
				}
				else
				{
					polar = -1;
				}
				
				if (word2polar.get(word) == null)
				word2polar.put(word, polar);
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public void loadSSTLexicon()
	{
		System.out.println("Loading SST Lexicon...");
		String filename = "data//sentiment//sst-lexicon//words.txt";
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(new FileInputStream(filename), "UTF-8");
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader br = new BufferedReader(isr);
		
		try {
			while(br.ready()){
				
				String line = br.readLine().trim();
				if(line.startsWith("##") || line.length() == 0){
					continue;
				}
				
				String[] fields = line.split(SEPERATOR);
				
				String word = fields[0];
				String Polar = fields[1];
				
				int polar = Integer.parseInt(Polar);
				
				
				if (word2polar.get(word) == null)
					word2polar.put(word, polar);
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	public void loadOpinionLexicon()
	{
		System.out.println("Loading opinion-lexicon Lexicon...");
		String lexiconPath = "data//sentiment//opinion-lexicon-English//";
		String[] filenames = new String[]{lexiconPath + "positive-words.txt", lexiconPath + "negative-words.txt"};
		int[] polarities = new int[]{1, -1};
				
		for(int i = 0; i < filenames.length; i++)
		{
			String fileName = filenames[i];
			int polar = polarities[i];
			
			
			InputStreamReader isr = null;
			try {
				isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
			} catch (UnsupportedEncodingException | FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BufferedReader br = new BufferedReader(isr);
			
			try {
				while(br.ready()){
					
					String line = br.readLine().trim();
					if(line.startsWith(";") || line.length() == 0){
						continue;
					}
					if (word2polar.get(line) == null)
					word2polar.put(line, polar);
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public void loadSent140()
	{
		System.out.println("Loading Sent 140 Lexicon...");
		String lexiconPath = "data//sentiment//";
		String[] filenames = new String[]{lexiconPath + "sentiment140.lex"};
		
		word2polar_double.clear();
			
		for(int i = 0; i < filenames.length; i++)
		{
			String fileName = filenames[i];
			
			
			
			InputStreamReader isr = null;
			try {
				isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
			} catch (UnsupportedEncodingException | FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BufferedReader br = new BufferedReader(isr);
			
			try {
				while(br.ready()){
					
					String line = br.readLine().trim();
					if(line.startsWith(";") || line.length() == 0){
						continue;
					}
					
					String[] tmp = line.trim().split(" ");
					String word = tmp[0];
					double score = Double.parseDouble(tmp[1]);
					
					if (word2polar_double.get(word) == null)
					word2polar_double.put(line, score);
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public Integer queryPolar(String word)
	{
		return this.word2polar.get(word);
	}
	
	public Double queryPolarDouble(String word)
	{
		return this.word2polar_double.get(word);
	}
	
	public Integer queryPolar(String word, char POSTag)
	{
		if (POSTag == 'N' || POSTag == 'V' || POSTag == 'A' || POSTag == 'R') {
			if (word.contains("-")) {
				if (word == "--" || word == "---") {
					return null;
				} else {
					String[] fields = word.split("-");
					if (fields.length == 2) {
						Integer polar = this.word2polar.get(fields[1]);
						if (polar != null) {
							if (fields[0] == "anti") {
								return polar * (-1);
							}
							else if (fields[0] == "too") {
								return polar * 2;
							}
							else {
								return polar;
							}
						}
						else {
							return null;
						}
					}
					else {
						return null;
					}
				}
			}
			else
			{
				return this.word2polar.get(word);
			}
		} else {
			return null;
		}
	}
	
	public void printStat()
	{
		System.out.println("Lexicons contain " + word2polar.keySet().size() + " words");
	}
}
