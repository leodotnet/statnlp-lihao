package org.statnlp.targetedsentiment.common;

import java.util.ArrayList;
import java.util.List;

import org.statnlp.commons.types.LinearInstance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.example.base.BaseInstance;


public class TSInstance<T> extends LinearInstance<T> {

	private static final long serialVersionUID = 1851514046050983662L;
	public boolean useAdditionalData = false;
	String sentence = null;
	public enum TSInstanceType {Entity_level, Sentence_level};
	public TSInstanceType type = TSInstanceType.Entity_level;
	public ArrayList<int[]> scopes = null;
	public ArrayList<Integer> potentialSentWord = new ArrayList<Integer>(); 
	List<T> outputBackup;
	
	public int numNULLTarget = 0;
	public String NULLTarget = null;
	public String NULLTargetPred = null;
	public ArrayList<int[]> targets = null; //entityBegin, entityEnd, Polarity
	public ArrayList<int[]> targets_pred = null;  //entityBegin, entityEnd, Polarity

	public enum PolarityType {
		positive, neutral, negative
		//PP, NP, PRT, ADVP, SBAR, ADJP, INTJ, VP, CONJP
		//subRoadno, country, town, roomno, city, poi, community, redundant, subroad, cellno, roadno, devZone, otherinfo, road, subpoi, person, district, assist, houseno, floorno, prov, subroadno, subRoad
	}
	
	public enum WORD_FEATURE_TYPES {
		_id_, ne, brown_clusters5, jerboa, brown_clusters3, _sent_, _sent_ther_, sentiment, postag
	};
	
	public TSInstance(int instanceId, double weight){
		this(instanceId, weight, null);
	}
	
	public TSInstance(int instanceId, double weight, ArrayList<String[]> input){
		this(instanceId, weight, input, null, null);
	}
	
	public TSInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output){
		this(instanceId, weight, input, output, null);
	}
	
	public TSInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output, ArrayList<T> prediction){
		super(instanceId, weight);
		this.input = input;
		this.output = output;
		this.prediction = prediction;
	}
	
	@Override
	public int size() {
		return this.input.size();
	}
	
	public List<String[]> duplicateInput(){
		return input;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<T> duplicateOutput() {
		ArrayList<T> o = (ArrayList<T>)this.output;
		return (ArrayList<T>)o.clone();
	}
	
	@Override
	public TSInstance duplicate() {
		TSInstance inst = (TSInstance)super.duplicate();
		inst.sentence = this.sentence;
		inst.useAdditionalData = this.useAdditionalData;
		inst.type = this.type;
		inst.scopes = this.scopes;
		inst.potentialSentWord = this.potentialSentWord;
		inst.NULLTarget = this.NULLTarget;
		inst.numNULLTarget = this.numNULLTarget;
		inst.targets = this.targets;
		return inst;
	}
	
	public void preprocess() {
		preprocess(false);
	}
	
	public void preprocess(boolean onlyWord)
	{
		this.potentialSentWord.clear();
		
		for (int i = 0; i < input.size(); i++) {
			if (!onlyWord) {
				String postag = input.get(i)[WORD_FEATURE_TYPES.postag.ordinal()];
				String sent = input.get(i)[WORD_FEATURE_TYPES._sent_.ordinal()];
				String sent_ther = input.get(i)[WORD_FEATURE_TYPES._sent_ther_.ordinal()];
				char postagRaw = postag.charAt(0);

				if (postagRaw == 'J' || postagRaw == 'V' || !sent.equals("_") || !sent_ther.equals("_")) {
					this.potentialSentWord.add(i);
				}
			} else {
				
				String word = input.get(i)[0].toLowerCase();
				char postag = input.get(i)[WORD_FEATURE_TYPES.postag.ordinal()].charAt(0);
				
				
				
				Double sentDoubleScore = TargetSentimentGlobal.SentDict.queryPolarDouble(word);
				
				Integer sentIntScore = TargetSentimentGlobal.SentDict.queryPolar(word);
				
				input.get(i)[WORD_FEATURE_TYPES._sent_.ordinal()] = (sentIntScore == null) ? "_" : ((sentIntScore > 0) ? "positive" : "negative");
				
				//if (TargetSentimentGlobal.SentDict.negation.contains(word)) {
					//input.get(i)[WORD_FEATURE_TYPES._sent_.ordinal()] = "negation";
				//}
				
				input.get(i)[WORD_FEATURE_TYPES._sent_ther_.ordinal()] = "_";
				
				boolean satifsPOSTAG = (postag == 'V' || postag == 'A' || postag == 'R' || postag == '!');
				
				if (TargetSentimentGlobal.USE_ALLWORDS_AS_CANDIDICATE || satifsPOSTAG || sentDoubleScore != null || sentIntScore != null)
					this.potentialSentWord.add(i);
			}

		}
	}
	
	public String getSentence()
	{
		if (this.sentence == null)
		{
			this.sentence = "";
			for(int i = 0; i < input.size(); i++)
			{
				String word = TargetSentimentGlobal.clean(input.get(i)[0]);
				this.sentence += word + " ";
			}
		}
		
		this.sentence = this.sentence.trim();
		
		return this.sentence;
	}
	
	public void setAdditional(boolean additional)
	{
		this.useAdditionalData = additional;
	}
	
	public void setAdditional()
	{
		this.useAdditionalData = true;
	}
	
	public void setSentenceLevelInstance()
	{
		this.type = TSInstanceType.Sentence_level;
	}
	
	public void setScopes(ArrayList<int[]> scopes) 
	{
		this.scopes = scopes;
	}
	
	public void setPredictionAsOutput() {
		this.outputBackup = this.output;
		this.output = this.prediction;
	}
	
	public void setTarget(ArrayList<int[]> targets) {
		this.targets = targets;
	}
	
	public void setTargetPrediction(ArrayList<int[]> targets_pred) {
		this.targets_pred = targets_pred;
	}
}
