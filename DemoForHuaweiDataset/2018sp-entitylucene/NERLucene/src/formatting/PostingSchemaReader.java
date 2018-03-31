package formatting;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class PostingSchemaReader {
	ArrayList<ArrayList<String>> retlist = new ArrayList<ArrayList<String>>(); 
	ArrayList<String> fieldname = new ArrayList<String>();
	ArrayList<String> typelist = new ArrayList<String>();
	String filelocation;
	
	public PostingSchemaReader(String locationarg){
		
		filelocation = locationarg; 
	}
	
	public ArrayList<ArrayList<String>> parseXML()
	{
		try
		{
			File fXmlFile = new File(filelocation);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			//optional, but recommended
			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			//System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

			NodeList nList = doc.getElementsByTagName("field");

			//System.out.println("----------------------------");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				//System.out.println("\nCurrent Element :" + nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					fieldname.add(eElement.getAttribute("name"));
					typelist.add(eElement.getAttribute("type"));
				//	System.out.println("Postings Name : " + eElement.getAttribute("name"));
				//	System.out.println("Type : " + eElement.getAttribute("type"));

				}
			}
			retlist.add(fieldname);
			retlist.add(typelist); 
			return retlist;
		}
		catch(Exception e)
		{
			System.out.println("XMLParsing Error");
			e.printStackTrace();
		}
		return null;
	}

}
