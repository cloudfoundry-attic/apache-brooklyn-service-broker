package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class PlaceholderReplacer {
	
	private Random random;

	public PlaceholderReplacer(Random random){
		this.random = random;
	}
	
	public Map<String, Object> replaceValues(Map<String, Object> map) {
		for(Entry<String, Object> e : map.entrySet()){
			if(e.getValue() instanceof Map){
				map.put(e.getKey(), replaceValues((Map<String, Object>)e.getValue()));
			} else if (e.getValue() instanceof String) {
				map.put(e.getKey(), replaceValue((String)e.getValue()));
			} else if (e.getValue() instanceof List) {
				map.put(e.getKey(), replaceValues((List<Object>)e.getValue()));
			}
		}
		return map;
	}
	
	public List<Object> replaceValues(List<Object> list) {
		List<Object> replacements = new ArrayList<>();
		for(Object o : list) {
			if(o instanceof Map){
				replacements.add(replaceValues((Map<String, Object>)o));
			} else if (o instanceof String) {
				replacements.add(replaceValue((String)o));
			} else if (o instanceof List) {
				replacements.add(replaceValues((List<Object>)o));
			}
		}
		list.clear();
		list.addAll(replacements);
		return list;
	}

	public String replaceValue(String value) {
		return value.replace("$(string.random)", randomString(8));
	}
	
	public String randomString(int length){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < length; i++){
			sb.append(random.nextInt('z' - '!') + '!');
		}
		return sb.toString();
	}

}
