
import java.io.IOException;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.lucene.queryparser.classic.ParseException;

import indexing.IndexingManager;
import indexing.Ingester;
import jline.console.ConsoleReader;
import jline.console.completer.AnsiStringsCompleter;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;
import ner.annotation.AnnotationManager_cmd;
import ner.annotation.EntityCatalog;

public class CommandLineConsole 
{
	
	private static Collection<Ingester> ingestionThreads;
	private static AnnotationManager_cmd annotationManager = new AnnotationManager_cmd();
	private static IndexingManager indexer = new IndexingManager("JSON", "JSON", true, 1000, false, new String[]{}, new String[]{});
	static int numThreads = 2;
	static int numDocs = 4;
	static int docsPerThread = numDocs/numThreads;
	static ExecutorService exec = Executors.newFixedThreadPool(numThreads);

	
    public static void usage() {
        System.out.println("  add entity: xxx - adds entities to be searched for");
        System.out.println("  commit - sets up the extraction modules before search ");
        System.out
            .println("  search - searches the index");
        System.out.println("\n  E.g - java Example simple su '*'\n"
            + "will use the simple compleator with 'su' triggering\n"
            + "the use of '*' as a password mask.");
    }
    
    public static void addEntityType(String Entity){
    	annotationManager.addEntityType_interface(Entity);
    }    
    
    public static void set_Annotator(){
    	ingestionThreads = new ArrayList<Ingester>();
    	
		for(int i = 0; i < numThreads; i++)
		{
			Ingester newIngester = new Ingester();
			newIngester.setAnnotator(annotationManager.getAnnotator());
			if(i == (numThreads-1) && docsPerThread*(i-1) < numDocs)
			{
				System.out.println(docsPerThread*i + ", " + numDocs);
				newIngester.setDocumentRange(docsPerThread*i, numDocs);
			}
			else
			{
				System.out.println(docsPerThread*i + ", " + docsPerThread*(i+1));
				newIngester.setDocumentRange(docsPerThread*i, docsPerThread*(i+1));
			}
			newIngester.setIndexingManager(indexer);
			ingestionThreads.add(newIngester);
		}
    }    
    
    public static void runQuery() throws IOException, InterruptedException, ExecutionException{
		DecimalFormat df = new DecimalFormat("###.###");
		df.setRoundingMode(RoundingMode.HALF_DOWN);

		AnnotationManager_cmd annotationManager = new AnnotationManager_cmd();
	//	IndexingManager indexer = new IndexingManager();

		long startTime = System.currentTimeMillis();
		List<Future<Double>> threadOut = exec.invokeAll(ingestionThreads);
		for(Future<Double> f : threadOut)
		{
			System.out.println(df.format(f.get()));
		}
		indexer.closeWriters();
		long endTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("INDEXED " + numDocs + " DOCS WITH " + numThreads + " THREAD(S) IN: " + df.format((endTime - startTime)*0.001) + " Seconds");
		exec.shutdown();
		try
		{
			System.out.println();
			System.out.println("Running query: 'entity_place'");
			indexer.runSampleQuery("entity_place");
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
    	
}
    

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        try {
            Character mask = '*';
            String search_trigger = "search";    
            String add_entity_trigger = "add entity:";
            String set_annotater_trigger = "commit";
            String get_entities_trigger = "show entity types";
            boolean color = true;

            ConsoleReader reader = new ConsoleReader();

            reader.setPrompt("EntityLucene> ");

            usage();


            List<Completer> completors = new LinkedList<Completer>();


//            if (args.length > 0) {
//                if (args[0].equals("none")) {
//                }
//                else if (args[0].equals("files")) {
//                    completors.add(new FileNameCompleter());
//                }
//                else if (args[0].equals("simple")) {
//                    completors.add(new StringsCompleter("foo", "bar", "baz"));
//                }
//                else if (args[0].equals("color")) {
//                    color = true;
//                    reader.setPrompt("\u001B[42mfoo\u001B[0m@bar\u001B[32m@baz\u001B[0m> ");
//                    completors.add(new AnsiStringsCompleter("\u001B[1mfoo\u001B[0m", "bar", "\u001B[32mbaz\u001B[0m"));
//                    CandidateListCompletionHandler handler = new CandidateListCompletionHandler();
//                    handler.setStripAnsi(true);
//                    reader.setCompletionHandler(handler);
//                }
//                else {
//                    usage();
//
//                    return;
//                }
//            }
            


//            if (args.length == 3) {
//                mask = '*';
//                trigger = "run";
//            }

            for (Completer c : completors) {
                reader.addCompleter(c);
            }

            String line;
            PrintWriter out = new PrintWriter(reader.getOutput());

            while ((line = reader.readLine()) != null) {
            	String[] line_array = line.split(" ");
            	
                out.println("======>\"" + line + "\"");
              
                out.flush();

                // If we input the special word then we will mask
                // the next line.
                if ((line_array[0].compareTo(search_trigger) == 0)) {
                    System.out.println("search trigger");
                    //line = reader.readLine();
                	 runQuery();	
                }
                
                else if ((line_array.length==3) && ((line_array[0]+" "+line_array[1]).compareTo(add_entity_trigger) == 0)) {
                        System.out.println("entity add trigger");
                    	String cur_entity = line_array[2];
                    	System.out.println(cur_entity + " added!");
                    	addEntityType(cur_entity);	
                }
                
                else if ((line.compareTo(set_annotater_trigger) == 0)) {
                    //line = reader.readLine();
                    System.out.println("set annotator trigger");
                    //line = reader.readLine();
                	 set_Annotator();
                }
                
                else if ((line.compareTo(get_entities_trigger) == 0)) {
                    //line = reader.readLine();
                    System.out.println("show entity types");
                    //line = reader.readLine();
                    System.out.print(annotationManager.getEntityTypes().toString());
                    System.out.println(); 
                }
                
                else if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
                else if (line.equalsIgnoreCase("cls")) {
                    reader.clearScreen();
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}