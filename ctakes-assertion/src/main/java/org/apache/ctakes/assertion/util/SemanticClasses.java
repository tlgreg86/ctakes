package org.apache.ctakes.assertion.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class SemanticClasses extends HashMap<String,HashSet<String>>{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  // loads files in the input directory into a hashmap that maps the filename minus the extension ("allergy.txt" becomes "allergy")
  // to the set of words in that file ("allergy" => ("allergic", "allergies", "allergy", ...)
  public SemanticClasses(InputStream inStream){
    Scanner scanner = new Scanner(inStream);
    while(scanner.hasNextLine()){
      String term = scanner.nextLine().trim();
      // if the term on this line is a multi-word expression, ignore, because we can't
      // place these in the tree anyways
      
      String[] keyVal = term.split("\t");
      if(keyVal.length != 2) continue;
      if(!this.containsKey(keyVal[0])){
        this.put(keyVal[0], new HashSet<String>());
      }
      
      if(!keyVal[1].contains(" ")){
        this.get(keyVal[0]).add(keyVal[1]);
      }
    }
  }
}
