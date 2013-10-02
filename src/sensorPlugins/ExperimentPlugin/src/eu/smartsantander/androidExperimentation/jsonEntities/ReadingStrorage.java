package eu.smartsantander.androidExperimentation.jsonEntities;

import java.util.HashMap;

public class ReadingStrorage {
	
	HashMap<String,Reading> store;
	
	public ReadingStrorage(){
		this.store=new HashMap<String,Reading>();
	}
	
	public void pushReading(Reading r){
		this.store.put(r.getContext(), r);
	}

	public Reading getReading(String context){
		return this.store.get(context);
	}
}
