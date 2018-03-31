package indexing;
import java.io.File;

import java.io.FileFilter;


/**
 * Specifies the files that will be indexed
 * @author aaulabaugh@gmail.com
 */
public class TextFileFilter implements FileFilter
{
	// https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm
	@Override
	public boolean accept(File f)
	{
		return f.getName().toLowerCase().endsWith(".txt");
	}

}
