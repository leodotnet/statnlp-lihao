package org.citationspan.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.commons.types.LinearInstance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.example.base.BaseInstance;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.negation.common.NegEval;
import org.statnlp.negation.common.NegationInstance;
import org.statnlp.negation.common.NegationGlobal;
import org.statnlp.negation.common.Utils;
import org.statnlp.negation.common.NegationInstance.FEATURE_TYPES;

public class CitationSpanInstance<T> extends LinearInstance<T> {

	
	public HashMap<String, Object> data = null;
	
	int[] sentenceCounter = null; 
	
	public String[] sentences = null;
	
	public ArrayList<int[]> cite_id_pos = null;
	
	public int[] outputsArr = null;
	
	public int[] citationArr = null;
	/**
	 * 
	 */
	private static final long serialVersionUID = -467366052560086888L;


	List<T> outputBackup = null;
	
	
	public int sentID = -1;


	public enum FEATURE_TYPES {
		word, MP
	};

	public CitationSpanInstance(int instanceId, double weight) {
		this(instanceId, weight, null);
	}

	public CitationSpanInstance(int instanceId, double weight, ArrayList<String[]> input) {
		this(instanceId, weight, input, null, null);
	}

	public CitationSpanInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output) {
		this(instanceId, weight, input, output, null);
	}

	public CitationSpanInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output, ArrayList<T> prediction) {
		super(instanceId, weight);
		this.input = input;
		this.output = output;
		this.prediction = prediction;
	}

	public void setOutput(ArrayList<T> output) {
		this.output = output;
	}

	@Override
	public int size() {
		return this.input.size();
	}

	public List<String[]> duplicateInput() {
		return input;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<T> duplicateOutput() {
		// ArrayList<T> o = (ArrayList<T>) this.output;
		// return (ArrayList<T>) o.clone();
		return null;
	}

	@Override
	public CitationSpanInstance duplicate() {
		CitationSpanInstance inst = (CitationSpanInstance) super.duplicate();
		inst.data = this.data;
		inst.sentenceCounter = this.sentenceCounter;
		inst.cite_id_pos = this.cite_id_pos;
		inst.outputsArr = this.outputsArr;
		inst.citationArr = this.citationArr;
		inst.sentences = this.sentences;
		return inst;
	}

	public void preprocess() {
		preprocess(false);
	}

	public void preprocess(boolean onlyWord) {
		
		this.citationArr = new int[input.size()];
		Arrays.fill(this.citationArr, 0);
		
		for(int[] pos : cite_id_pos) {
			citationArr[pos[0]] = 1;
		}
		
	}

	public String getSentence() {
		return null;
	}

	

	public void setPredictionAsOutput() {
		this.outputBackup = this.output;
		this.output = this.prediction;
	}

	public String getSentID() {
		return this.sentID + "";
	}

	

	
}
