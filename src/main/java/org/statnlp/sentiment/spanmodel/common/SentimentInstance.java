package org.statnlp.sentiment.spanmodel.common;

import java.util.ArrayList;
import java.util.List;

import org.statnlp.commons.types.LinearInstance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.example.base.BaseInstance;
import org.statnlp.sentiment.spanmodel.SpanModelCompiler.*;
import org.statnlp.sentiment.spanmodel.globalinfo.SpanModelCompiler.OPNodeType;
import org.statnlp.sentiment.spanmodel.globalinfo.SpanModelCompiler.SentimentStateType;

public class SentimentInstance<T> extends BaseInstance<SentimentInstance<T>, List<String>, Integer> {

	private static final long serialVersionUID = 1851514046050983662L;
	
	String sentence = null;
	public ArrayList<Integer> predictSentiment = new ArrayList<Integer>();
	public ArrayList<Integer> predictOP = new ArrayList<Integer>();
	public String explanation = "";
	//public ArrayList<int[]> predictTextSpan = new ArrayList<int[]>();
	public ArrayList<String> POSTags = null;
	
	public SentimentInstance(int instanceId, double weight){
		this(instanceId, weight, null);
	}
	
	public SentimentInstance(int instanceId, double weight, ArrayList<String> input){
		this(instanceId, weight, input, null, null);
	}
	
	public SentimentInstance(int instanceId, double weight, ArrayList<String> input, Integer output){
		this(instanceId, weight, input, output, null);
	}
	
	public SentimentInstance(int instanceId, double weight, ArrayList<String> input, Integer output, Integer prediction){
		super(instanceId, weight);
		this.input = input;
		this.output = output;
		this.prediction = prediction;
	}
	
	@Override
	public int size() {
		return this.input.size();
	}
	
	
	public List<String> duplicateInput(){
		return input;
	}
	
	@SuppressWarnings("unchecked")
	public Integer duplicateOutput() {
		Integer o = (Integer)this.output;
		return new Integer(o);
	}
	
	@Override
	public SentimentInstance duplicate() {
		SentimentInstance inst = super.duplicate();
		inst.textSpanList = this.textSpanList;
		inst.sentimentWordList = this.sentimentWordList;
		inst.sentence = this.sentence;
		inst.POSTags = this.POSTags;
		return inst;
	}
	
	public void setPosTag(ArrayList<String> POSTags)
	{
		this.POSTags = POSTags;
	}
	public String getSentence()
	{
		return this.sentence;
	}
	
	public void setSentence(String sentence)
	{
		this.sentence = sentence;
	}
	
	public ArrayList<int[]> textSpanList = new ArrayList<int[]>();
	
	public ArrayList<Integer> sentimentWordList = new ArrayList<Integer>();
	
	public void preprocess(SentimentDict dict)
	{
		textSpanList.clear();
		sentimentWordList.clear();
		int lastBoundary = 0;
		int size = input.size();
		for(int i = 0;i < size; i++)
		{
			String word = input.get(i).toLowerCase();
			String POSTag = this.POSTags.get(i);
			Integer s = dict.queryPolar(word,POSTag.charAt(0));
			if (s != null)
			{
				textSpanList.add(new int[]{lastBoundary, i});   //[lastBoundary, i)  
				sentimentWordList.add(i);   					//input.get(i) is the sentiment word
				lastBoundary = i + 1;
			}
		}
		
		if (lastBoundary < size)
		{
			textSpanList.add(new int[]{lastBoundary, size});  //[lastBoundary, size)
		}
	}
	
	public boolean useInstance()
	{
		/*
		if (sentimentWordList.isEmpty())
		{
			return false;
		}*/
		
		//sentimentWordList.add(output);
		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		
		for(int i = 0; i < this.predictOP.size(); i++)
		{
			int OPIdx = this.predictOP.get(i);
			sb.append(OPNodeType.values()[OPIdx].name() + " ");
		}
		sb.append("\n");
		
		for(int i = 0; i < this.predictSentiment.size(); i++)
		{
			
			
			int sentimentStateIdx = predictSentiment.get(i);
			sb.append(SentimentStateType.values()[sentimentStateIdx].name() + " ");
			
			
		}
		sb.append("\n");
		
		return sb.toString();
	}
	
	public String getExplanation()
	{
		StringBuffer sb = new StringBuffer();
		for(int j = 0; j < predictSentiment.size(); j++)
		{
			String sentimentState = SentimentStateType.values()[(Integer)predictSentiment.get(j)].name();
			String OP = OPNodeType.values()[(Integer)predictOP.get(j)].name();
			String OP_prefix = OP.substring(0, OP.indexOf("_"));
			String textspan = "";
			
			if (j >= textSpanList.size()) continue;
			
			int[] textspanIndex = (int[]) textSpanList.get(j);
			
			for(int k = textspanIndex[0]; k < textspanIndex[1]; k++)
			{
				String word =  input.get(k);
				textspan += word + " ";
				
				//if (!SpanModelGlobal.highFreqWords.contains(word))
					//counter.addWordForClass(word, OP_prefix);
			}
			
			if (j < predictSentiment.size() - 1 && textspanIndex[1] < input.size())
			{
				textspan += input.get(textspanIndex[1]);
			}
			
			sb.append("[" + sentimentState + "] ");
			sb.append(textspan);
			sb.append(" [" + OP + "] ");
			
			/*
			if (j == predictSentiment.size() - 1)
			{
				predict_2ndlast[i] = (Integer)inst.predictSentiment.get(j);
			}*/
			
		}
		
		//sb.append(predict[i] + " " + gold[i]);
		return sb.toString();
	}
}
