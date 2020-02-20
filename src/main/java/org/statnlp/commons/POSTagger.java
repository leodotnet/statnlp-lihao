package org.statnlp.commons;
import java.util.Arrays;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class POSTagger {
	
	static MaxentTagger tagger = null;

	/*
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String a = "I like watching movies";
		MaxentTagger tagger =  new MaxentTagger("posmodel//english-left3words-distsim.tagger");
		System.out.println(Arrays.toString(getPOSTags(tagger, a)));

	}*/
	
	public static void loadModel() {
		tagger =  new MaxentTagger("pos-models//english-left3words-distsim.tagger");
	}
	
	public static String[] getPOSTags(String sentence) {
		if (tagger == null) {
			loadModel();
		}
		
	
		String tagged = tagger.tagString(sentence);
		String[] token_tagged = tagged.split(" ");
		
		String[] tags = new String[token_tagged.length];
		
		for(int i = 0; i < tags.length; i++) {
			tags[i] = token_tagged[i].split("_")[1];
		}
		
		return tags;
	}

}
