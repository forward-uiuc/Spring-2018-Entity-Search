package ner.annotation.extraction.huawei_tagging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.Map.Entry;
public class QueryTaggingAutoServer {

	
	public static boolean isNumeric(String str){  
		  for (int i = str.length();--i>=0;){    
		   if (!Character.isDigit(str.charAt(i))){  
		    return false;  
		   }  
		  }  
		  return true;  
		} 
	
	
	 
	// 判断一个字符是否是中文  
	    public static boolean isChinese(char c) {  
	        return c >= 0x4E00 &&  c <= 0x9FA5;// 根据字节码判断  
	    }  
	    // 判断一个字符串是否含有中文  
	    public static boolean isChinese(String str) {  
	        if (str == null) return false;  
	        for (char c : str.toCharArray()) {  
	            if (isChinese(c)) return true;// 有一个中文字符就返回  
	        }  
	        return false;  
	    }
	public static String questiontagbyAlldictionaryNoOverlapAuto(String []paths,String outputPath, String InputPath) throws Exception
	{
			String dicIndexPath=new DictionaryAnalysis().allDictionaryToTableByAuto(paths);
			
			String version="";
		    for(String one: paths)
		    {
		    	if(one.contains("product"))
		    	{
		    		//Q:\\huawei\\dictionary\\dictionaryv3\\product_v6.txt
		    		version=one.split("_")[1];
		    		version=version.replace("v","");
		    		version=version.replace(".txt","");
		    				
		    		
		    	}
		    }
				
			FileReader fileinput = null;
				BufferedReader br=null;
			Map<String,List<String>> phrase2infoFourDic=new HashMap<String,List<String>>();

			Map<String,List<String>> phrase2infoProfessionalDic=new HashMap<String,List<String>>();
			Set<String> importantDoc=new HashSet<String>();
		
						
				try {
					fileinput = new FileReader(dicIndexPath);
					br=new BufferedReader(fileinput);
					String s=null;
					
					while((s=br.readLine())!=null){
						s=s.toLowerCase();
						String phraseno=s.split("\t")[0];
						String phrase=s.split("\t")[1];
						String phraseroot=s.split("\t")[2];
						String phraselabel=s.split("\t")[3];
						String phrasetype=s.split("\t")[4];
						if(phraselabel.contains("product")||phraselabel.contains("symptom")||phraselabel.contains("signalword")
								||phraselabel.equals("a")||phraselabel.contains("a<#>")||phraselabel.contains("<#>a")
								||phraselabel.equals("h")||phraselabel.contains("h<#>")||phraselabel.contains("<#>h")
								||phraselabel.equals("f")||phraselabel.contains("f<#>")||phraselabel.contains("<#>f"))
							{
							
							String phraseversion=s.split("\t")[5];
							List<String> list=new ArrayList<String>();
							list.add(phraseno);
							list.add(phraseroot);
							if(phraselabel.contains("<#>professionalword"))
							{
								phraselabel=phraselabel.replace("<#>professionalword", "");
							}
							if(phraselabel.contains("professionalword<#>"))
							{
								phraselabel=phraselabel.replace("professionalword<#>", "");
							}
							
							if(phraselabel.contains("<#>generalwords"))
							{
								phraselabel=phraselabel.replace("<#>generalwords", "");
							}
							if(phraselabel.contains("generalwords<#>"))
							{
								phraselabel=phraselabel.replace("generalwords<#>", "");
							}
							
							list.add(phraselabel);
							list.add(phrasetype);
							list.add(phraseversion);
							if(!isNumeric(phrase))
							{
								phrase2infoFourDic.put(phrase, list);
								importantDoc.add(phrase);
							}

						}
											
					}
					
					fileinput = new FileReader(dicIndexPath);
					br=new BufferedReader(fileinput);
				
					
					while((s=br.readLine())!=null){
						//125	互联网电视	互联网电视	professionalwords<#>accessory	针对性同义词*<#>重点词*<#>相关名词<#>	2017-11-30
						s=s.toLowerCase();
						String phraseno=s.split("\t")[0];
						String phrase=s.split("\t")[1];
						String phraseroot=s.split("\t")[2];
						String phraselabel=s.split("\t")[3];
						String phrasetype=s.split("\t")[4];
						 if(phraselabel.equals("professionalword")||phraselabel.equals("generalwords")||phraselabel.equals("generalwords<#>professionalword")||phraselabel.equals("professionalword<#>generalwords")){
							String phraseversion=s.split("\t")[5];
							List<String> list=new ArrayList<String>();
							list.add(phraseno);
							list.add(phraseroot);
							list.add(phraselabel);
							list.add(phrasetype);
							list.add(phraseversion);
							if(importantDoc.contains(phrase))
							{
								continue;
							}
							//if(phrase.length()>=2)// 单个中文不要
							//{
								phrase2infoProfessionalDic.put(phrase, list);
								
							//}
							
							//phrase2infoOtherDic.put(phrase, list);
							
						}
											
					}
					
					//System.out.println("phrase2infoProfessionalDic \t"+ phrase2infoProfessionalDic.size());
					
				}catch (Exception e) {
					
					e.printStackTrace();
				}
				finally
				{
					if(br!=null)
					{
					try {
						br.close();
					} catch (IOException e) {
						
						e.printStackTrace();
					}
					br=null;
					}
					
					//System.out.println("success=========================ptxe");
				}

		    BufferedWriter bwscall=null;
		    OutputStreamWriter  scall=new OutputStreamWriter(new FileOutputStream(outputPath),"UTF-8");

		    String tagResultPath=outputPath;
		    bwscall=new BufferedWriter(scall);
		
			try {
				
				InputStreamReader	read = new InputStreamReader(new FileInputStream(new File(InputPath)),"UTF-8"); 
				
				br=new BufferedReader(read);
				
				String s="";
			
				while((s=br.readLine())!=null)
				{
					s=s.toLowerCase();
					if(s.split("\t").length<=1)
					{
						continue;
					}
					String standard=s.split("\t")[0]; // 1 for answer
					//String qid=s.split("<&&>")[1];11
					//qid2queryterm.put(standard, qid);
					Set<String> set=new HashSet<String>();
					if(standard!=null&!standard.equals(""))
					{
											
						
						//从前往后最长切分，
						Map<Integer,String> startPosition2TermsImportantDic=new TreeMap<Integer,String>();
						for(int i=0;i<standard.length();i++)
						{
							int position=i;
							for(int j=i+1;j<=standard.length();j++)
							{
								String phrase=standard.substring(i,j);
								//System.out.println(phrase);
								//
								if(phrase2infoFourDic.containsKey(phrase))
								{
									startPosition2TermsImportantDic.put(i, phrase);
									position=j-1;
								}
								
							}
							i=position;
						}
						
						for(Entry<Integer,String> en:startPosition2TermsImportantDic.entrySet())
						{
							//set.add(en.getValue()+"<&&>"+en.getKey());
							int position=en.getKey();
							String phrase=en.getValue();
							//String qid=qid2queryterm.get(standard);
							/*if(qid==null)
							{
								//System.out.println(standard);
								continue;
							}*/
						
							List<String> phraseinfo=phrase2infoFourDic.get(phrase);
							
														
							String phraseid=phraseinfo.get(0);
							String phraselabel=phraseinfo.get(2);
							
							
							bwscall.write(new String("".getBytes("UTF-8"))+standard+"\t"+phrase+"\t"+phraseid+"\t"+position+"\t"+phraselabel+"\n");
							bwscall.flush();
							// 
						}
						Map<String,Integer> sentence2Position=new HashMap<String,Integer>();
						List<String> sentenceWithoutTaggingByImportantDic=new ArrayList<String>();
						int end=0;
						for(Entry<Integer,String> en:startPosition2TermsImportantDic.entrySet())
						{
							int startposition=en.getKey();
							String content=en.getValue();
							int endposition=startposition+content.length();
							
							String sen=standard.substring(end,startposition);

							if(sen!=null&&!sen.equals(""))
							{
								sentence2Position.put(sen, end);
								sentenceWithoutTaggingByImportantDic.add(sen);
								
							}
							end=endposition;
							
							// 
						}
						
						String sen=standard.substring(end,standard.length());
						//System.out.println(sen);
						if(sen!=null&&!sen.equals(""))
						{
							sentenceWithoutTaggingByImportantDic.add(sen);
							sentence2Position.put(sen, end);
						}
						
					
						if(sentenceWithoutTaggingByImportantDic.size()>0)
						{
							for(String sentence: sentenceWithoutTaggingByImportantDic)
							{
								int senposition=sentence2Position.get(sentence);
								Map<Integer,String> startPosition2TermsProfessionalDic=new HashMap<Integer,String>();
								for(int i=0;i<sentence.length();i++)
								{
									int position=i;
									for(int j=i+1;j<=sentence.length();j++)
									{
										String phrase=sentence.substring(i,j);

										if(phrase2infoProfessionalDic.containsKey(phrase))
										{
											startPosition2TermsProfessionalDic.put(senposition+i, phrase);
											position=j-1;
										}

									}
									i=position;
								}
							
								for(Entry<Integer,String> en:startPosition2TermsProfessionalDic.entrySet())
								{
									int position=en.getKey();
									String phrase=en.getValue();
									//String qid=qid2queryterm.get(standard);
								
									/*if(qid==null)
									{
									
										continue;
									}*/
									List<String> phraseinfo=phrase2infoProfessionalDic.get(phrase);

									
									
									String phraseid=phraseinfo.get(0);
									String phraselabel=phraseinfo.get(2);
									
									
									//bwscall.write(standard+"\t"+phrase+"\t"+phraseid+"\t"+position+"\t"+phraselabel+"\n");
									//bwscall.write(new String(standard+"\t"+phrase+"\t"+phraseid+"\t"+position+"\t"+phraselabel.getBytes("UTF-8"))+"\n");
									//bwscall.write(new String(standard.getBytes("UTF-8")+"\t"+phrase+"\t"+phraseid+"\t"+position+"\t"+phraselabel)+"\n");
									//bwscall.write(new String(standard.getBytes("UTF-8"),"UTF-8")+"\t"+phrase+"\t"+phraseid+"\t"+position+"\t"+phraselabel+"\n");
									bwscall.write(new String("".getBytes("UTF-8"))+standard+"\t"+phrase+"\t"+phraseid+"\t"+position+"\t"+phraselabel+"\n");
									bwscall.flush();
								}
							}
						}						
					}
										
				}
								
			
			} catch (Exception e) {
				
				e.printStackTrace();
			}
			finally
			{
				if(br!=null)
				{
				try {
					br.close();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
				br=null;
				}
				
				//System.out.println("success=========================ptxe");
			}

					    
			return tagResultPath;

}
	public void runAnnotation(String[] dictionaryPaths, String outputPath, String inputPath)
	{
		try
		{
			//System.out.println(outputPath);
			String s = outputPath.split("\\.")[0];
			//System.out.println(s);
			String output=s+"temp.txt";
			String tempPath=questiontagbyAlldictionaryNoOverlapAuto(dictionaryPaths,output,inputPath);
			writeFinalTagsNoOverLapAuto(inputPath, tempPath,outputPath);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	
	
	public static void writeFinalTagsNoOverLapAuto(String orgInputPath, String path,String outputPath) throws Exception, IOException
	{
		
		FileReader fileinput = null;
		BufferedReader br=null;
		Map<String,List<EScore>> qidqueryterm2Tags=new HashMap<String,List<EScore>>();
		Map<String,String> question2Answer=new HashMap<String,String>();
		try {
			fileinput = new FileReader(orgInputPath);
			br=new BufferedReader(fileinput);
			String s=null;
			
			while((s=br.readLine())!=null){
				
				String question=s.split("\t")[0];
				String answer=s.split("\t")[1];
				question2Answer.put(question, answer);
			}
	
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		finally
		{
			if(br!=null)
			{
			try {
				br.close();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			br=null;
			}
			
			System.out.println("success=========================ptxe");
		}

		try {
			fileinput = new FileReader(path);
			br=new BufferedReader(fileinput);
			String s=null;
			
			while((s=br.readLine())!=null){
				s=s.toLowerCase();
				String query=s.split("\t")[0];
				String pharse=s.split("\t")[1];
				String pharseid=s.split("\t")[2];
				
				double position=Double.parseDouble(s.split("\t")[3]);
				String label=s.split("\t")[4];
				String labelno="k";
				if(label.equals("professionalword")||label.equals("generalwords")||label.equals("generalwords<#>professionalword")||label.equals("professionalword<#>generalwords"))
				{
					labelno="k";
				}
				else
				{
					if(label.contains("signalword"))
					{
						label=label.replace("signalword", "w");
					}
					
					if(label.contains("symptom"))
					{
						label=label.replace("symptom", "s");
					}
					
					if(label.contains("product"))
					{
						label=label.replace("product", "p");
					}
					if(label.contains("<#>"))
					{
						label=label.replace("<#>", "&");
					}
					
					labelno=label;
					
				}
				
				List<EScore> list=new ArrayList<EScore>();
				if(qidqueryterm2Tags.containsKey(query))
				{
					list=qidqueryterm2Tags.get(query);
				}
				
				EScore en=new EScore();
				en.setE(pharse+"_"+labelno+"_"+(int)position);
				//en.setE(pharse+"_"+labelno);
				en.setScore(1.0/(1.0+position));
				list.add(en);
				
				qidqueryterm2Tags.put(query, list);
				
			}
		
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		finally
		{
			if(br!=null)
			{
			try {
				br.close();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			br=null;
			}
			
			System.out.println("success=========================ptxe");
		}
		
		BufferedWriter bwscfour=null;
	    OutputStreamWriter  scfour=new OutputStreamWriter(new FileOutputStream(outputPath),"UTF-8");
		
	    
	    bwscfour=new BufferedWriter(scfour);
	    
	    for(Entry<String,List<EScore>> entry: qidqueryterm2Tags.entrySet())
	    {
	    	String qidqueryterm=entry.getKey();
	    	List<EScore> list=entry.getValue();
	    	Collections.sort(list);
	    	StringBuffer sb=new StringBuffer();
	    	for(EScore en: list)
	    	{
	    		if(sb.length()==0)
	    		{
	    			sb.append(en.getE());
	    		}
	    		else
	    		{
	    			sb.append("#").append(en.getE());
	    		}
	    	}
	    	String content=sb.toString();
	    	String pattern="";
	    	    	 
	    	 List<String> pattern1=new ArrayList<String>();
	    	 for(EScore en: list)
	 	    	{
	 	    		String note=en.getE().split("_")[1];
	 	    		if(note.equals("k"))
	 	    		{
	 	    			continue;
	 	    		}

	 	    		
	 	    		pattern1.add(note);
	 	    		
	 	    	}
	    	 StringBuffer sb2=new StringBuffer();
	    	 if(pattern1.size()>=1)
	    	 {
		    	 for(String one: pattern1)
		    	 {
		    		 if(sb2.length()==0)
		    		 {
		    			 sb2.append(one);
		    		 }
		    		 else
		    		 {
		    			 sb2.append(one);
		    		 }
		    	 }
	    	 }
	    	 
	    	 String answer=question2Answer.get(qidqueryterm);
	    
	    	 bwscfour.write(new String(qidqueryterm.getBytes("UTF-8"))+"\t"+sb.toString()+"\t"+pattern1.size()+"\t"+sb2.toString()+"\t"+answer+"\n");
	    	 bwscfour.flush();
	    }
		
		
	}
	
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String []paths=new String[8];
		String inputpath="/Users/apple/sp18/EntitySearch/intermediate/intermediate_0.txt";
		String outputpath="/Users/apple/sp18/EntitySearch/StandardTestQuestion_QuestionIDLabelResults.txt";
		//String tempfileName="/Users/apple/sp18/EntitySearch/StandardTestQuestion_QuestionIDLabelResults1.txt";
		paths[0]="/Users/apple/sp18/EntitySearch/dictionaries/generalwords.v2.csv";
		paths[1]="/Users/apple/sp18/EntitySearch/dictionaries/professionalword_v13.txt";
		paths[2]="/Users/apple/sp18/EntitySearch/dictionaries/product_v13.txt";
		paths[3]="/Users/apple/sp18/EntitySearch/dictionaries/symptom_v13.txt";
		paths[4]="/Users/apple/sp18/EntitySearch/dictionaries/A_v13.txt";
		paths[5]="/Users/apple/sp18/EntitySearch/dictionaries/H_v13.txt";
		paths[6]="/Users/apple/sp18/EntitySearch/dictionaries/F_v13.txt";
		paths[7]="/Users/apple/sp18/EntitySearch/dictionaries/signalword_v13.txt";		
		//String path=questiontagbyAlldictionaryNoOverlapAuto(paths,tempfileName,inputpath);
		//writeFinalTagsNoOverLapAuto(path,outputpath);
		QueryTaggingAutoServer tagger = new QueryTaggingAutoServer();
		tagger.runAnnotation(paths, outputpath, inputpath);
	}

}

class DictionaryAnalysis {
	public static String allDictionaryToTableByAuto(String[]paths) throws Exception
	{
		Map<String,Set<String>> phrasetype=new HashMap<String,Set<String>>();
		Map<String,Set<String>> phraseLabel=new HashMap<String,Set<String>>();
		Map<String,String> type2index=new TreeMap<String,String>();
		BufferedWriter bwsc=null;
		String version="";
		String productdic="";
	    for(String one: paths)
	    {
	    	if(one.contains("product"))
	    	{
	    		productdic=one;

	    		version=one.split("_")[1];
	    		version=version.replace("v","");
	    		version=version.replace(".txt","");
	    				
	    		
	    	}
	    }
	    File fi=new File(productdic+"1");
	    String directPath=fi.getParent();
	    String dicIndexPath=directPath+"/alldictionaryindex_v"+version+".txt";
		
	    OutputStreamWriter  sc=new OutputStreamWriter(new FileOutputStream(dicIndexPath),"UTF-8");
	    bwsc=new BufferedWriter(sc);
        FileReader fileinput = null;
		BufferedReader br=null;
		Set<String> phrases=new HashSet<String>();
		Map<String,Set<String>> phrase2alias=new HashMap<String,Set<String>>();
		Map<String,Map<String,String>> alias2Properties=new HashMap<String,Map<String,String>>();
		
		int no=0;
		
			try {
				for(String path: paths)
				{
				InputStreamReader read =null;
							
				String s=null;
				
				File file=new File(path);
				 read = new InputStreamReader(new FileInputStream(file),"UTF-8"); 
				 br=new BufferedReader(read);
			
			
				String filename=file.getName();
				if(filename.indexOf(".")>0)
				{
					filename=filename.split("\\.")[0];
				}
				filename=filename.split("_")[0];
				String lastlexicalname="";
				while((s=br.readLine())!=null)
				{
					s=s.toLowerCase();
					//分类一	分类二	分类三	分类四	词类名	同义词
					//table: dictionary: phrase id--->phrase--》词根-》分类信息--> lable(such as, product, application)--> version
					
					
					if(s.split("\t").length<5)
					{
						continue;
						//System.out.println(s);
					}
					
					String t1=s.split("\t")[0].trim();
					String t2=s.split("\t")[1].trim();
					String t3=s.split("\t")[2].trim();
					String t4=s.split("\t")[3].trim();
					String type=t1+"<#>"+t2+"<#>"+t3+"<#>"+t4;
					String lexicalname=s.split("\t")[4];
					no++;
					if(lexicalname!=null&&!lexicalname.equals(""))
					{
						Set<String> set=new HashSet<String>();
						if(phrasetype.containsKey(lexicalname))
						{
							set=phrasetype.get(lexicalname);
						}
						set.add(type);
						phrasetype.put(lexicalname, set);
						
						Set<String> labelSet=new HashSet<String>();
						if(phraseLabel.containsKey(lexicalname))
						{
							labelSet=phraseLabel.get(lexicalname);
						}
						
						
						
						labelSet.add(filename);
						phraseLabel.put(lexicalname, labelSet);
						
						Map<String,String> map=new HashMap<String,String>();
						if(alias2Properties.containsKey(lexicalname))
						{
							map=alias2Properties.get(lexicalname);
						}
						String aliasroot=lexicalname;
						map.put("phraseroot", aliasroot);
						if(map.containsKey("label"))
						{
							String label=map.get("label");
							if(!label.contains(filename))
							{
								map.put("label", map.get("label")+"<#>"+filename);
							}
						}
						else
						{
							map.put("label", filename);
						}
						
						if(map.containsKey("type"))
						{
							map.put("type", map.get("type")+"<#>"+type);
						}
						else
						{
							map.put("type", type);
						}
						
						alias2Properties.put(lexicalname, map);
						
						
						
						
						
						lastlexicalname=lexicalname;
					}
					else
					{
						String alias=s.split("\t")[5];
						if(alias!=null&&!alias.equals(""))
						{
							// alias filename
							Map<String,String> map=new HashMap<String,String>();
							if(alias2Properties.containsKey(alias))
							{
								map=alias2Properties.get(alias);
							}
							String aliasroot=lastlexicalname;
							map.put("phraseroot", aliasroot);
							if(map.containsKey("label"))
							{
								String label=map.get("label");
								if(!label.contains(filename))
								{
									map.put("label", map.get("label")+"<#>"+filename);
								}
								
							}
							else
							{
								map.put("label", filename);
							}
							
							if(map.containsKey("type"))
							{
								map.put("type", map.get("type")+"<#>"+type);
							}
							else
							{
								map.put("type", type);
							}
							
							alias2Properties.put(alias, map);
													
						}
						
						
					}
					
					
				
				
				}
				
				}
				
				
				int count=0;
				for(Entry<String,Map<String,String>> entry: alias2Properties.entrySet())
				{
					String alias=entry.getKey();
					Map<String,String> map=entry.getValue();
					String label=map.get("label");
					String type=map.get("type");
					String phraseroot=map.get("phraseroot");
					count++;
					bwsc.write(count+"\t"+new String(alias.getBytes("UTF-8"))+"\t"+new String(phraseroot.getBytes("UTF-8"))+"\t"+label+"\t"+type+"\t"+version+"\n");
					bwsc.flush();
				}
				
				
				
			
			} catch (Exception e) {
				
				e.printStackTrace();
			}
			finally
			{
				if(br!=null)
				{
				try {
					br.close();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
				br=null;
				}
				
				//System.out.println("success=========================ptxe");
			}
		  				
    	return dicIndexPath;

}	
	
}



class EScore implements Serializable, Comparable<EScore>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -822718322651956415L;
	/**
	 * 
	 */

	public EScore(String e) {
		super();
		this.e = e;
	
	}
	public boolean isMatch() {
		return match;
	}
	public void setMatch(boolean match) {
		this.match = match;
	}
	public boolean match=false;
	public String id;
	public String getId() {
		return id;
	}
	
	public String getDocId() {
		return docId;
	}
	public void setDocId(String docId) {
		this.docId = docId;
	}
	public String docId;
	
	
	
	public void setId(String id) {
		this.id = id;
	}
	
	
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String e;
	public String category;
	
	public String getE() {
		return e;
	}
	public EScore() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public EScore(String e, double score) {
		super();
		this.e = e;
		this.score = score;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((e == null) ? 0 : e.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EScore other = (EScore) obj;
		if (e == null) {
			if (other.e != null)
				return false;
		} else if (!e.equals(other.e))
			return false;
		
		return true;
	}
	
	
	@Override
	public String toString() {
		return "[e=" + e + ", score=" + score + "]";
	}
	public void setE(String e) {
		this.e = e;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public double score;
	public int compareTo(EScore o) {
		// TODO Auto-generated method stub
		
		EScore t=o;
		 return Double.compare(t.getScore(),this.getScore());
	}
	
	
	
}
