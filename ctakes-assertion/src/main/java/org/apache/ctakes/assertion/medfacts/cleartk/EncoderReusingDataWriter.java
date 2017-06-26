package org.apache.ctakes.assertion.medfacts.cleartk;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.google.common.collect.Lists;
import de.bwaldvogel.liblinear.FeatureNode;
import org.cleartk.ml.Feature;
import org.cleartk.ml.encoder.CleartkEncoderException;
import org.cleartk.ml.encoder.features.FeaturesEncoder;
import org.cleartk.ml.encoder.outcome.StringToIntegerOutcomeEncoder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.cleartk.ml.liblinear.encoder.FeatureNodeArrayEncoder;
import org.cleartk.util.collection.GenericStringMapper;
import org.cleartk.util.collection.Writable;

public class EncoderReusingDataWriter extends LibLinearStringOutcomeDataWriter {

  public EncoderReusingDataWriter(File outputDirectory)
      throws FileNotFoundException {
    super(outputDirectory);
    File encoderFile = new File(outputDirectory, "encoders.ser");
    if(encoderFile.exists()){
      try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(encoderFile));
        this.setFeaturesEncoder(new WritingFeatureNodeArrayEncoder((FeatureNodeArrayEncoder) ois.readObject()));
        ois.close();
      } catch (ClassNotFoundException | IOException e) {
        e.printStackTrace();
        throw new FileNotFoundException("Problem loading encoder from encoders.ser");
      }
    }else{
      this.setFeaturesEncoder(new WritingFeatureNodeArrayEncoder());
    }
    
    File outputEncoderFile = new File(outputDirectory, "outcome-lookup.txt");
    if(outputEncoderFile.exists()){
      StringToIntegerOutcomeEncoder outcomeEncoder = new StringToIntegerOutcomeEncoder();
      try(Scanner scanner = new Scanner(outputEncoderFile)){
        String line;
        while(scanner.hasNextLine()){
          line = scanner.nextLine();
          String[] ind_val = line.split(" ");
          outcomeEncoder.encode(ind_val[1]);
        }
      }
      this.setOutcomeEncoder(outcomeEncoder);
    }
  }

  public static class WritingFeatureNodeArrayEncoder implements FeaturesEncoder<FeatureNode[]> {
    private FeatureNodeArrayEncoder encoder = null;
    private Set<String> featureNames = null;
    public static final String LOOKUP_FILE_NAME = "features-lookup.txt";

    public WritingFeatureNodeArrayEncoder() {
      encoder = new FeatureNodeArrayEncoder();
      featureNames = new HashSet<>();
    }

    public WritingFeatureNodeArrayEncoder(FeatureNodeArrayEncoder encoder){
      this.encoder = encoder;
      featureNames = new HashSet<>();
    }

    @Override
    public FeatureNode[] encodeAll(Iterable<Feature> features) throws CleartkEncoderException {
      FeatureNode[] encoded = encoder.encodeAll(features);
      for(Feature feature : features){
        String name;
        if(feature.getValue() instanceof Number) {
          name = feature.getName();
        } else {
          name = Feature.createName(new String[]{feature.getName(), feature.getValue().toString()});
        }
        featureNames.add(name);
      }
      return encoded;
    }

    @Override
    public void finalizeFeatureSet(File file) throws IOException {
      encoder.finalizeFeatureSet(file);

      File outFile = new File(file.getPath(), this.LOOKUP_FILE_NAME);
      PrintWriter out = new PrintWriter(new FileWriter(outFile));
      for(String featName : featureNames){
        List<Feature> feat = Lists.newArrayList(new Feature(featName, 1.0));
        try {
          FeatureNode encodedNode = encoder.encodeAll(feat)[1]; // index 0 is the bias feature
          out.println(String.format("%s : %d", featName, encodedNode.getIndex()));
        }catch(CleartkEncoderException e){
          throw new IOException(e);
        }
      }
    }
  }
}
