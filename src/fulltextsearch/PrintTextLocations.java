package fulltextsearch;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package org.apache.pdfbox.examples.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * Parse a given PDF document and search for occurrences of a given term.
 * Print to system out, information needed to fuel BookReader's search feature.
 * The data includes relevant page and text information like text coordinates.
 */
public class PrintTextLocations extends PDFTextStripper
{
	
	private static String searchterm;
	private static int termlength;
	private static String style;
	private static List<List> allMatches = new ArrayList<>();
	private static List<List> lineBounds = new ArrayList<>();
	private static List output = new ArrayList();
	
    /**
     * Instantiate a new PDFTextStripper object.
     *
     * @throws IOException If there is an error loading the properties.
     */
    public PrintTextLocations() throws IOException
    {
    	
    }

    /**
     * This will print the documents data.
     *
     * @param args The command line arguments.
     *
     * @throws IOException If there is an error parsing the document.
     */
    public static void main( String[] args ) throws IOException
    {
        if( args.length < 5 )
        {
            usage();
        }
        else
        {
            PDDocument document = null;
            try
            {
            	String ia = args[0];
            	String path = args[1];
            	String query = args[2].trim();
            	String callback = args[3];
            	style = args[4];
            	
            	File file = new File(path);
            	searchterm = query.toLowerCase();
            	termlength = searchterm.length();
                document = PDDocument.load(file);

                PDFTextStripper stripper = new PrintTextLocations();
                stripper.setSortByPosition( true );
                stripper.setStartPage( 0 );
                stripper.setEndPage( document.getNumberOfPages() );

                Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
                stripper.writeText(document, dummy);

                printInfo(document.getNumberOfPages(), ia, callback);
                document.close();
                
            } 
            catch (Exception e) {
            	System.out.println("Hmmm, looks like something went wrong.");
            	usage();
            }
            finally
            {
                if( document != null )
                {
                    document.close();
                }
            }
        }
    }
    

    /**
     * Override the default functionality of PDFTextStripper.
     * 
     * Performs a line-by-line, char-by-char search on the pdf document.
     * Creates a list of lists of details of the the term found.
     * Details include
     * - the coordinates of each char found that matches searchterm
     * - the page #, page height and page width of the page it was found in
     * - the coordinates of the boundaries of the line searchterm was found in
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException
    {
    	List temp = new ArrayList();
    	
    	float pagewidth = textPositions.get(0).getPageWidth();
    	float pageheight = textPositions.get(0).getPageHeight();
    	
    	String lower = string.toLowerCase();
    	String normalized = Normalizer.normalize(lower, Form.NFD).replaceAll("[\\p{M}]", "");
    	int prevIndex = 0;
    	int start = 0;
    	
    	// Search through the original and an non-accented version of the text
    	while (lower.indexOf(searchterm, prevIndex) >= 0 || normalized.indexOf(searchterm, prevIndex) >= 0) {
    		
    		if (lower.indexOf(searchterm, prevIndex) >= 0) {
        		start = lower.indexOf(searchterm, prevIndex);
    		}
    		else {
        		start = normalized.indexOf(searchterm, prevIndex);
    		}
    		
    		int fin = start + termlength;
    		
    		// Grab the coordinates of only the term
    		float x = textPositions.get(start).getXDirAdj();
    		float y = textPositions.get(start).getYDirAdj();
    		float x2 = textPositions.get(fin - 1).getXDirAdj();
    		float y2 = textPositions.get(fin - 1).getYDirAdj();
    		float height = textPositions.get(start).getFontSize();
    		float width = textPositions.get(fin - 1).getWidthDirAdj();
    		
    		List coords = formatCoords(x, y - height, x2 + width, y);
    		allMatches.add(coords);
    		
    		// Grab the coordinates for the whole line containing the term
    		int lastitem = textPositions.size() - 1;
    		float leftBound = textPositions.get(0).getXDirAdj();
    		float rightBound = textPositions.get(lastitem).getXDirAdj() + textPositions.get(lastitem).getWidthDirAdj();
    		float topBound = y - height;
    		float botBound = y;
    		
    		// Build the new string with the {{{highlighting braces}}}
        	String newStr = string.replaceAll("\"", "");
        	String begin = newStr.substring(0, start);
        	String mid = newStr.substring(start, fin);
        	String end = newStr.substring(fin);
        	newStr = begin + "{{{" + mid + "}}}" + end;
			
    		lineBounds.add(Arrays.asList(newStr, getCurrentPageNo(), pagewidth, pageheight, botBound, topBound, rightBound, leftBound));
    		prevIndex = fin;
    	}
    }
    
    
    /**
     * This will print the usage for this document.
     */
    private static void usage()
    {
    	System.err.println( "Usage: java -jar pdfbox_search.jar <item-id> <file-path> <query-term> <callback> <css-or-abbyy>" );
    }
    
    
    /**
     * Return the set of coordinates in either CSS style of Abbyy style.
     * (Defaults to abbyy)
     * - CSS uses x, y, width, height.
     * - Abbyy uses x1, y1, x2, y2.
     */
    public List formatCoords(float x1, float y1, float x2, float y2) {
    	List coords = new ArrayList();
    	float r, b;
    	float l = x1;
    	float t = y1;
    	
    	if (style.equals("css")) {
    		// r is width, b is height
    		r = x2 - l;
    		b = y2 - t;
    	}
    	else {
    		r = x2;
    		b = y2;
    	}
    	
    	coords.add(r);
    	coords.add(b);
    	coords.add(t);
    	coords.add(l);
    	
    	return coords;
    }
    
    
    /**
     * Print the contents of allMatches and lineBounds to stdout.
     * Every term found contains a block of info separated by a newline.
     * 
     * The info is the same as printJson but formatted differently to make
     * reading and parsing easier.
     * 
     * @param totalPages
     * @param ia
     * @param callback
     */
    public static void printInfo(int totalPages, String ia, String callback) {
    	List temp = new ArrayList();
    	
    	System.out.println("callback:" + callback
    			+ "\nia:" + ia
    			+ "\nterm:" + searchterm
    			+ "\npages:" + totalPages + "\n");
        
        for (int i = 0; i < allMatches.size(); i++) {
			List match = allMatches.get(i);
			List bounds = lineBounds.get(i);
        	
        	Object text = bounds.get(0);
        	int pgnum = (int) bounds.get(1) - 1;
        	float pgwidth = (float) bounds.get(2);
        	float pgheight = (float) bounds.get(3);
        	float bBound = (float) bounds.get(4);
        	float tBound = (float) bounds.get(5);
        	float rBound = (float) bounds.get(6);
        	float lBound = (float) bounds.get(7);
        	
        	float r = (float) match.get(0);
        	float b = (float) match.get(1);
        	float t = (float) match.get(2);
        	float l = (float) match.get(3);
        	
        	System.out.println("text:" + text 
        			+ "\npage_num:" + pgnum 
        			+ "\npage_size:" + pgwidth + "," + pgheight
        			+ "\ntext_bounds:" + bBound + "," + tBound + "," + rBound + "," + lBound
        			+ "\nterm_bounds:" + r + "," + b + "," + t + "," + l + "\n");
        }
    }
    
    
    /*
     * ------------------- All code under these lines are unused -------------------
     * They were built for previous iterations and performed slightly different tasks.
     */
    
    
    /**
     * Write out the contents of allMatches and lineBounds to a new file "output.txt".
     * Every term found contains a block of info separated by a newline.
     * 
     * The info is the same as printJson but formatted differently to make
     * reading and parsing easier. An attempt to speed up the process but
     * printInfo is faster.
     * 
     * (writeInfo is currently unused)
     * @param totalPages
     * @param ia
     * @param callback
     */
    public static void writeInfo(int totalPages, String ia, String callback) throws FileNotFoundException, UnsupportedEncodingException {
    	PrintWriter writer = new PrintWriter("./output.txt", "UTF-8");
    	List temp = new ArrayList();
    	
    	writer.println("callback:" + callback
    			+ "\nia:" + ia
    			+ "\nterm:" + searchterm
    			+ "\npages:" + totalPages + "\n");
        
        for (int i = 0; i < allMatches.size(); i++) {
			List match = allMatches.get(i);
			List bounds = lineBounds.get(i);
        	
        	Object text = bounds.get(0);
        	int pgnum = (int) bounds.get(1) - 1;
        	float pgwidth = (float) bounds.get(2);
        	float pgheight = (float) bounds.get(3);
        	float bBound = (float) bounds.get(4);
        	float tBound = (float) bounds.get(5);
        	float rBound = (float) bounds.get(6);
        	float lBound = (float) bounds.get(7);
        	
        	float r = (float) match.get(0);
        	float b = (float) match.get(1);
        	float t = (float) match.get(2);
        	float l = (float) match.get(3);
        	
        	writer.println("text:" + text 
        			+ "\npage_num:" + pgnum 
        			+ "\npage_size:" + pgwidth + "," + pgheight
        			+ "\ntext_bounds:" + bBound + "," + tBound + "," + rBound + "," + lBound
        			+ "\nterm_bounds:" + r + "," + b + "," + t + "," + l + "\n");
        }
        writer.close();
    }
    
    
    /**
     * Create a new file called coords.txt that stores the results from the search.
     * Each search term found contains its own line of information.
     * 
     * (writeOut is currently unused)
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public static void writeOut() throws FileNotFoundException, UnsupportedEncodingException {
    	
    	PrintWriter writer = new PrintWriter("coords.txt", "UTF-8");
    	writer.println("Searched for: " + searchterm + " ");
    	
    	for (int i = 0; i < output.size(); i++) {
    		List line = (List) output.get(i);
    		
    		for (int j = 0; j < line.size(); j++) {
    			Object l = line.get(j);
    			writer.print(l.toString());
    			
    			if (j + 1 < line.size() || i % 2 < 1) {
    				writer.print(",");
    			}
    		}
    		
    		if (i + 1 < output.size() && i % 2 == 1) {
    			writer.println();
			}
    	}
    	
    	writer.close();
    	System.out.println("Created coords.txt.");
    }
    
    
    /**
     * Builds and writes out to a new json file the contents of our work.
     * In our case, the json file is a bookreader's search api call.
     * 
     * (buildJson is currently unused)
     * @param totalPages
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public static void buildJson(int totalPages, String ia, String callback) throws FileNotFoundException, UnsupportedEncodingException{
    	
    	PrintWriter writer = new PrintWriter("filename", "UTF-8");
    	List temp = new ArrayList();
    	
    	writer.println(callback + "( {"
        		+ "\n\t\"ia\": \"" + ia +  "\","
        		+ "\n\t\"q\": \"\\\"" + searchterm + "\\\"\","
        		+ "\n\t\"page_count\": " + totalPages + ","
        		+ "\n\t\"leaf0_missing\": true,"
        		+ "\n\t\"matches\": [");
        
        for (int i = 0; i < allMatches.size(); i++) {
			List match = allMatches.get(i);
			List bounds = lineBounds.get(i);
        	
        	Object text = bounds.get(0);
        	int pgnum = (int) bounds.get(1) - 1;
        	float pgwidth = (float) bounds.get(2);
        	float pgheight = (float) bounds.get(3);
        	float bBound = (float) bounds.get(4);
        	float tBound = (float) bounds.get(5);
        	float rBound = (float) bounds.get(6);
        	float lBound = (float) bounds.get(7);
        	
        	float r = (float) match.get(0);
        	float b = (float) match.get(1);
        	float t = (float) match.get(2);
        	float l = (float) match.get(3);
        	
        	writer.print("{" + "\n\t\"text\":\"" + text + "\", " 
        			+ "\n\t\"par\": [{" 
        			+ "\n\t\t\"page\": " + pgnum + ", \"page_width\": " + pgwidth + ", \"page_height\": " + pgheight + ","
        			+ "\n\t\t\"b\": " + bBound + ", \"t\": " + tBound + ", \"r\": " + rBound + ", \"l\": " + lBound + ","
        			+ "\n\t\t\"boxes\": ["
        			+ "\n\t\t\t{\"r\": " + r + ", \"b\": " + b + ", \"t\": " + t + ", \"l\": " + l + "}");
        	if (i + 1 < allMatches.size()) {
        		writer.println("\n\t\t] \n\t}] \n},");
        	}
        	else {
        		writer.println("\n\t\t] \n\t}] \n}");
        	}
        }
        
        writer.println("] \n} )");
        writer.close();
    	System.out.println("Created filename.txt.");
    }
    
    
    /**
     * Print the contents of allMatches and lineBounds to stdout in json format.
     * In our case, the json file is a bookreader's search api call.
     * 
     * (printJson is currently unused)
     * @param totalPages
     * @param ia
     * @param callback
     */
    public static void printJson(int totalPages, String ia, String callback){
    	List temp = new ArrayList();
    	
    	System.out.println(callback + "( {"
        		+ "\n\t\"ia\": \"" + ia +  "\","
        		+ "\n\t\"q\": \"\\\"" + searchterm + "\\\"\","
        		+ "\n\t\"page_count\": " + totalPages + ","
        		+ "\n\t\"leaf0_missing\": true,"
        		+ "\n\t\"matches\": [");
        
        for (int i = 0; i < allMatches.size(); i++) {
			List match = allMatches.get(i);
			List bounds = lineBounds.get(i);
        	
        	Object text = bounds.get(0);
        	int pgnum = (int) bounds.get(1) - 1;
        	float pgwidth = (float) bounds.get(2);
        	float pgheight = (float) bounds.get(3);
        	float bBound = (float) bounds.get(4);
        	float tBound = (float) bounds.get(5);
        	float rBound = (float) bounds.get(6);
        	float lBound = (float) bounds.get(7);
        	
        	float r = (float) match.get(0);
        	float b = (float) match.get(1);
        	float t = (float) match.get(2);
        	float l = (float) match.get(3);
        	
        	System.out.print("{" + "\n\t\"text\": \"" + text + "\", " 
        			+ "\n\t\"par\": [{" 
        			+ "\n\t\t\"page\": " + pgnum + ", \"page_width\": " + pgwidth + ", \"page_height\": " + pgheight + ","
        			+ "\n\t\t\"b\": " + bBound + ", \"t\": " + tBound + ", \"r\": " + rBound + ", \"l\": " + lBound + ","
        			+ "\n\t\t\"boxes\": ["
        			+ "\n\t\t\t{\"r\": " + r + ", \"b\": " + b + ", \"t\": " + t + ", \"l\": " + l + "}");
        	if (i + 1 < allMatches.size()) {
        		System.out.println("\n\t\t] \n\t}] \n},");
        	}
        	else {
        		System.out.println("\n\t\t] \n\t}] \n}");
        	}
        }
        
        System.out.println("] \n} )");
    }

}



