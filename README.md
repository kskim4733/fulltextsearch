# PDFBox Fulltext Search

Instructions to package fulltextsearch's PrintTextLocations.java as a jar for use with IA BookReader. Fulltextsearch uses the Apache PDFBox library to parse a given PDF document and search for a given term. Specifically fulltextsearch will retrieve data about the search term such as it's text coordinates and page number.


### Dependencies

Get Eclipse (I used Oxygen v4.7.0)
link: https://www.eclipse.org/downloads/

Get the following Apache PDFBox libraries (I used v2.0.9)
link: https://pdfbox.apache.org/
- debugger-app-2.0.9.jar
- fontbox-2.0.9.jar
- pdfbox-2.0.9.jar
- pdfbox-app-2.0.9.jar
- pdfbox-tools-2.0.9.jar
- preflight-2.0.9.jar
- preflight-app-2.0.9.jar
- xmpbox-2.0.9.jar


### Instructions

Using Eclipse:

1. First get a copy of the project and open it up on Eclipse.
	- In Eclipse, click `File -> Open Projects from File System`.
	- Open `.../path/to/fulltextsearch`.

2. Configure the build path to include libraries.
	- Open the Package Explorer View: `Window -> Show View -> Package Explorer`.
	- Drop-down fulltextsearch and right-click `Referenced Libraries` in Package Explorer.
	- Go to `Build Path -> Configure Build Path`.
	- Under the `Libraries` tab, click `Add External JARs`.
	- Add all the PDFBox libraries mentioned above.

3. Run `PrintTextLocations.java`
	- Locate in `fulltextsearch/src/fulltextsearch/PrintTextLocations.java` in the Package Explorer.
	- Run the application and the console should output a message: `Usage: java -jar...`
	- **Note** the message is actually an error message but is the expected output in this case.

4. Export `PrintTextLocations` as a runnable JAR file.
	- `File -> Export`
	- In the Java folder, choose `Runnable JAR file`.
	- The `Launch configuration` should be set to `PrintTextLocations - fulltextsearch`.
	- The destination should be in the same folder that includes the `search_inside.php` file.
		- To keep things easy, you should name the jar file `pdfbox_search.jar`.
		- If you use some other name, you must change `$cmd = "java -jar pdfbox_search.jar ...` in `search_inside.php` to use the name you've given it.
	- Under Library handling, select `Extract required libraries into generated JAR`.
	- Hit finished and you should now have an executable JAR file

5. Running the executable jar (optional).
	- If you would like to run the jar file to see the output, all you will need is a PDF.
	- Open up `cmd` and `cd` into the folder containing the jar file.
	- Enter `java -jar pdfbox_search.jar <item-id> <file-path> <query-term> <callback> <css-or-abbyy>` replacing the items in <> brackets with values.
		- The only values that need to be "real" or "truthful" are `<file-path>`, the path to the pdf file, and `<query-term>`, the text you are trying to look up.
		- The other values do not have meaning in a local demo, with the exception of `<css-or-abbyy>`, however the following will default to 'abbyy' whenever 'css' is not entered.
