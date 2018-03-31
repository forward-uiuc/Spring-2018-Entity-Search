package query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import formatting.TokenField;
import ner.annotation.EntityAnnotation;

/**
 * Aggregate results by their name and sort them.
 * @author jjiang1110@gmail.com
 */

public class Aggregator {
	
	public HashMap<String, Float> map;
	public LinkedList<Entry<String, Float>> list;
	public LinkedList<TopDocs> tds;
	
	public Aggregator(LinkedList<TopDocs> tds, LinkedList<Integer> validDocID) {
		this.tds = tds;
		map = new HashMap();
		for(TopDocs topDocs : tds){
			for(ScoreDoc sd : topDocs.scoreDocs){
				if(validDocID != null){
					if(!validDocID.contains(sd.doc))
						continue;
				}
		 		EntityScoreDoc esd = (EntityScoreDoc)sd;
		 		EntityAnnotation entity = esd.entity;
		 		String name = entity.getContent();
		 		if(map.containsKey(name))
		 			map.put(name, map.get(name) + esd.score);
		 		else
		 			map.put(name, esd.score);
			}
		}
		
		Set<Entry<String, Float>> set = map.entrySet();
		list = new LinkedList(set);
        Collections.sort(list, new Comparator<Map.Entry<String, Float>>()
        {
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2)
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        });
	}

	public String topEntity(){
		return list.get(0).getKey();
	}
	
	public float topScore(){
		return list.get(0).getValue();
	}
	
	@Override
	public String toString(){
		String ret = "";
		for(Map.Entry<String, Float> entry : list){
            ret += entry.getKey() + " ---- " + entry.getValue() + "\n";
        }
		return ret;
	}
	
}
