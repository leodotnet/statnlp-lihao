package org.entityrelation.common;

import java.util.ArrayList;
import java.util.List;
import org.statnlp.example.base.BaseInstance;


public class EntityRelationInstance<T> extends BaseInstance<EntityRelationInstance<T>, List<String[]>, T> {	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -467366052560086888L;



	String sentence = null;
	public AttributedWord[] attriWords;
	public int[] depIdx;
	public String[][] depPathToken = null;
	public String[][] depPathDepLabel = null;
	public int[][][] depPath = null;
	public T backupOutput = null;
	public int sentenceId = -1;
	public int numEntityInBetween[][];
	

	public enum FEATURE_TYPES {
		word, posTag, depIdx, depType
	};

	public EntityRelationInstance(int instanceId, double weight) {
		this(instanceId, weight, null);
	}

	public EntityRelationInstance(int instanceId, double weight, ArrayList<String[]> input) {
		this(instanceId, weight, input, null, null);
	}

	public EntityRelationInstance(int instanceId, double weight, ArrayList<String[]> input, T output) {
		this(instanceId, weight, input, output, null);
	}

	public EntityRelationInstance(int instanceId, double weight, ArrayList<String[]> input, T output, T prediction) {
		super(instanceId, weight);
		this.input = input;
		this.output = output;
		this.prediction = prediction;
	}


	@Override
	public int size() {
		return this.input.size();
	}

	public List<String[]> duplicateInput() {
		return input;
	}

	@SuppressWarnings("unchecked")
	public T duplicateOutput() {
		// ArrayList<T> o = (ArrayList<T>) this.output;
		// return (ArrayList<T>) o.clone();
		return this.output;//(T)((EntityRelationOutput)this.output).duplicate();
	}

	@Override
	public EntityRelationInstance duplicate() {
		EntityRelationInstance inst = (EntityRelationInstance) super.duplicate();
		inst.sentence = this.sentence;
		inst.attriWords = this.attriWords;
		inst.depIdx = this.depIdx;
		inst.depPathToken = this.depPathToken;
		inst.depPathDepLabel = this.depPathDepLabel;
		inst.depPath = this.depPath;
		inst.backupOutput = this.backupOutput;
		inst.sentenceId = this.sentenceId;
		inst.numEntityInBetween = this.numEntityInBetween;
		return inst;
	}
	
	public void copyInput(EntityRelationInstance inst) {
		this.input = (ArrayList<String[]>)inst.input;
		this.sentence = inst.sentence;
		this.attriWords = inst.attriWords;
		this.depIdx = inst.depIdx;
		this.depPathToken = inst.depPathToken;
		this.depPathDepLabel = inst.depPathDepLabel;
		this.depPath = inst.depPath;
		this.backupOutput = (T)inst.backupOutput;
		this.sentenceId = inst.sentenceId;
		this.numEntityInBetween = inst.numEntityInBetween;
	}

	public void preprocess() {
		preprocess(false);
	}
	
	public void setFlip(int size) {
		this.input = Utils.flipList(this.input);
		//flip dep indx
		for(int i = 0; i < size; i++) {
			String[] f = input.get(i);
			if (depIdx[i] != -1) {
				depIdx[i] = size - 1 - depIdx[i];
			}
			f[2] = depIdx[i] + "";
			depIdx[i] = Integer.parseInt(f[2]);
		}
		
		depIdx = Utils.flipArray(depIdx);
		
		((EntityRelationOutput)output).setFlip(size);
	}

	public void preprocess(boolean flag) {
		
		
		int size = this.size();
		
		if (depIdx == null) {
			depIdx = new int[this.input.size()];
			for(int i = 0; i < depIdx.length; i++) {
				depIdx[i] = Integer.parseInt(this.input.get(i)[2]);
			}
		}
		
		if (EntityRelationGlobal.FLIP) {
			this.setFlip(size);
		}
		
		
		if (sentence == null) {
			sentence = Utils.getPhrase(this.input, 0, this.size(), 0);
		}
		
		if (attriWords == null) {
			attriWords = new AttributedWord[this.input.size()];
			for(int i = 0; i < attriWords.length; i++) {
				attriWords[i] = new AttributedWord(this.input.get(i)[0]);
			}
		}
		
		
		
		EntityRelationOutput output = (EntityRelationOutput)this.output;
		
//		depPathToken = new String[size][size];
//		depPathDepLabel = new String[size][size]; 
		
		depPath = new int[size][size][];
		
		
//		for(int i = 0; i < size; i++) {
//			for(int k = 0; k < size; k++) {
//				getDep(i, k);
//			}
//		}
		
		int numEntity = output.entities.size();
		numEntityInBetween = new int[numEntity][numEntity];
		for(int i = 0; i < numEntity; i++) {
			Entity leftSpan = output.entities.get(i);
			for(int j = i + 1; j < numEntity; j++) {
				Entity rightSpan = output.entities.get(j);
				for (int k = i + 1; k < j; k++) {
					Entity e = output.entities.get(k);
					if (e.span[0] > leftSpan.span[1] && e.span[1] < rightSpan.span[0]) {
						numEntityInBetween[i][j]++;
					}
				}
			}
 		}
		
	}
	
	public int[] getDep(int i, int k) {
		if (depPath[i][k] == null) {
			int[] path = Utils.getShortestDepPath((EntityRelationInstance)this, (ArrayList<String[]>)this.input, i, k);
			depPath[i][k] = path;
//			depPathToken[i][k] = Utils.getPhrase(this.input, path);
//			depPathDepLabel[i][k] = Utils.getPhrase(this.input, path, 3, false);
		}
		
		return depPath[i][k];
	}

	public String getSentence() {
		if (sentence == null) {
			preprocess();
		}
		return sentence;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < this.input.size(); i++) {
			sb.append(input.get(i)[0] + " ");
		}
		sb.append("\n");
		sb.append(this.output);
		sb.append("\n");
		sb.append(this.prediction);
		sb.append("\n");
		return sb.toString();
	}
	
	public void backup() {
		this.backupOutput = this.output;
	}

	public void revertBackup() {
		if (this.backupOutput != null) {
			this.output = this.backupOutput;
			this.backupOutput = null;
		}
	}
	
}
