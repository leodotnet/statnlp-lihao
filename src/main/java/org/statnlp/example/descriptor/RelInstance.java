package org.statnlp.example.descriptor;

import org.statnlp.example.base.BaseInstance;

public class RelInstance extends BaseInstance<RelInstance, CandidatePair, RelationDescriptor> {

	private static final long serialVersionUID = 6971863806062822950L;
	
	public RelInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}

	public RelInstance(int instanceId, double weight, CandidatePair input, RelationDescriptor output) {
		this(instanceId, weight);
		this.input = input;
		this.output = output;
	}


	@Override
	public int size() {
		return this.input.sent.length();
	}
	
	public CandidatePair duplicateInput(){
		return input;
	}
	
	public RelationDescriptor duplicateOutput() {
		return this.output;
	}

}





