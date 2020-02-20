package org.statnlp.example.descriptor.emb;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.statnlp.commons.io.RAWF;
import org.statnlp.commons.ml.opt.MathsVector;

public class TurianWordEmbedding implements WordEmbedding{

	private Map<String, double[]> lookupTable;
	
	public TurianWordEmbedding(String file) {
		System.out.println("[Google] Loading Turian embeddings....");
		this.readEmbedding(file);
		System.out.println("[Google] Finish reading Turian embeddings.");
		System.out.println("[Google] If a word appear in embedding but not in training data, we still use the embedding");
	}
	
	public void readEmbedding(String file) {
		lookupTable = new HashMap<>();
		BufferedReader br;
		try {
			br = RAWF.reader(file);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] vals = line.split(" ");
				String word = vals[0];
				double[] emb = new double[50];
				for (int i = 0; i < emb.length; i++) {
					emb[i] = Double.valueOf(vals[i + 1]);
				}
				double norm = MathsVector.norm(emb);
				for (int i = 0; i < emb.length; i++) {
					emb[i] /= norm;
				}
				this.lookupTable.put(word, emb);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double[] getEmbedding(String word) {
		return lookupTable.containsKey(word) ?  lookupTable.get(word) : lookupTable.get("*UNKNOWN*");
	}
	
	public void clearEmbeddingMemory() {
		this.lookupTable.clear();
		this.lookupTable = null;
	}

	@Override
	public int getDimension() {
		return 50;
	}
}
