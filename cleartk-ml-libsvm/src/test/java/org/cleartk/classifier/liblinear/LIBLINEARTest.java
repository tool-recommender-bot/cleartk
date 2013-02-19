/** 
 * Copyright (c) 2010, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package org.cleartk.classifier.liblinear;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.ScoredOutcome;
import org.cleartk.classifier.jar.DefaultDataWriterFactory;
import org.cleartk.classifier.jar.DirectoryDataWriterFactory;
import org.cleartk.classifier.jar.Train;
import org.cleartk.classifier.liblinear.encoder.FeatureNodeArrayEncoder;
import org.cleartk.classifier.libsvm.ExampleInstanceFactory;
import org.cleartk.classifier.libsvm.ExampleInstanceFactory.BooleanAnnotator;
import org.cleartk.classifier.libsvm.ExampleInstanceFactory.StringAnnotator;
import org.cleartk.test.DefaultTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.uimafit.factory.UimaContextFactory;
import org.uimafit.testing.util.HideOutput;

import com.google.common.collect.Lists;

import de.bwaldvogel.liblinear.FeatureNode;

/**
 * <br>
 * Copyright (c) 2010, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Philip Ogren
 * 
 */

public class LIBLINEARTest extends DefaultTestBase {

  @Test
  public void testBooleanOutcomeLIBLINEAR() throws Exception {
    // create the data writer
    BooleanAnnotator annotator = new BooleanAnnotator();
    annotator.initialize(UimaContextFactory.createUimaContext(
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        this.outputDirectoryName,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        LIBLINEARBooleanOutcomeDataWriter.class.getName()));

    // run process to produce a bunch of instances
    annotator.process(null);

    annotator.collectionProcessComplete();

    // check that the output file was written and is not empty
    LIBLINEARBooleanOutcomeClassifierBuilder builder = new LIBLINEARBooleanOutcomeClassifierBuilder();
    BufferedReader reader = new BufferedReader(new FileReader(
        builder.getTrainingDataFile(this.outputDirectory)));
    Assert.assertTrue(reader.readLine().length() > 0);
    reader.close();

    // run the training command
    HideOutput hider = new HideOutput();
    Train.main(this.outputDirectoryName, "-c", "1.0", "-s", "1");
    hider.restoreOutput();

    // read in the classifier and test it on new instances
    LIBLINEARBooleanOutcomeClassifier classifier;
    classifier = builder.loadClassifierFromTrainingDirectory(this.outputDirectory);
    for (Instance<Boolean> instance : ExampleInstanceFactory.generateBooleanInstances(1000)) {
      List<Feature> features = instance.getFeatures();
      Boolean outcome = instance.getOutcome();
      Assert.assertEquals(outcome, classifier.classify(features));
      ScoredOutcome<Boolean> scoredOutcome = classifier.score(features, 2).get(0);
      Assert.assertEquals(outcome, scoredOutcome.getOutcome());
      Assert.assertTrue(scoredOutcome.getScore() > 0.0d);
    }
  }
  
  @Test
  public void testStringOutcomeLIBLINEAR() throws Exception {
    // create the data writer
    StringAnnotator annotator = new StringAnnotator();
    annotator.initialize(UimaContextFactory.createUimaContext(
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        this.outputDirectoryName,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        LIBLINEARStringOutcomeDataWriter.class.getName()));

    // run process to produce a bunch of instances
    annotator.process(null);

    annotator.collectionProcessComplete();

    // check that the output files were written for each class
    BufferedReader reader = new BufferedReader(new FileReader(new File(
        this.outputDirectoryName,
        "training-data.liblinear")));
    Assert.assertTrue(reader.readLine().length() > 0);
    reader.close();

    // run the training command
    HideOutput hider = new HideOutput();
    Train.main(this.outputDirectoryName, "-c", "1.0", "-s", "0");
    hider.restoreOutput();

    // read in the classifier and test it on new instances
    LIBLINEARStringOutcomeClassifierBuilder builder = new LIBLINEARStringOutcomeClassifierBuilder();
    LIBLINEARStringOutcomeClassifier classifier;
    classifier = builder.loadClassifierFromTrainingDirectory(this.outputDirectory);
    for (Instance<String> instance : ExampleInstanceFactory.generateStringInstances(1000)) {
      List<Feature> features = instance.getFeatures();
      String outcome = instance.getOutcome();
      Assert.assertEquals(outcome, classifier.classify(features));

      List<ScoredOutcome<String>> scoredOutcomes = classifier.score(features, 1);
      Assert.assertEquals(1, scoredOutcomes.size());
      Assert.assertEquals(outcome, scoredOutcomes.get(0).getOutcome());
      scoredOutcomes = classifier.score(features, 2);
      Assert.assertEquals(2, scoredOutcomes.size());
      Assert.assertTrue(scoredOutcomes.get(0).getScore() > scoredOutcomes.get(1).getScore());
      scoredOutcomes = classifier.score(features, Integer.MAX_VALUE);
    }
  }
  
  @Test
  public void testMajorityClass() throws Exception {
    // training data has 4 times as many AAA as BBB
    LIBLINEARStringOutcomeDataWriter dataWriter = new LIBLINEARStringOutcomeDataWriter(this.outputDirectory);
    dataWriter.write(new Instance<String>("AAA", Lists.newArrayList(new Feature("V"))));
    dataWriter.write(new Instance<String>("AAA", Lists.newArrayList(new Feature("W"))));
    dataWriter.write(new Instance<String>("AAA", Lists.newArrayList(new Feature("X"))));
    dataWriter.write(new Instance<String>("AAA", Lists.newArrayList(new Feature("Y"))));
    dataWriter.write(new Instance<String>("BBB", Lists.newArrayList(new Feature("Z"))));
    dataWriter.finish();
    
    LIBLINEARStringOutcomeClassifierBuilder classifierBuilder = dataWriter.getClassifierBuilder();
    classifierBuilder.trainClassifier(this.outputDirectory);
    classifierBuilder.packageClassifier(this.outputDirectory);
    
    // test on a feature never seen during training
    LIBLINEARStringOutcomeClassifier classifier = classifierBuilder.loadClassifierFromTrainingDirectory(this.outputDirectory);
    Assert.assertEquals("AAA", classifier.classify(Lists.newArrayList(new Feature("A"))));
  }
  
  @Test
  public void testFeatureNodeArrayEncoder() throws Exception {
    FeatureNode[] expected = new FeatureNode[] {
        new FeatureNode(1, 1.0), /* bias */
        new FeatureNode(2, 1.0),
        new FeatureNode(3, 42.0)
    };
    
    // create an encoder, add some features and then finalize the feature set
    FeatureNodeArrayEncoder encoder = new FeatureNodeArrayEncoder();
    Assert.assertArrayEquals(expected, encoder.encodeAll(Arrays.asList(
        new Feature("dog"),
        new Feature("badger", 42))));
    encoder.finalizeFeatureSet(null);
    
    // test that multiple features with the same index are combined
    Assert.assertArrayEquals(
        new FeatureNode[] { new FeatureNode(1, 1) /* bias */, new FeatureNode(2, 2.0) },
        encoder.encodeAll(Arrays.asList(new Feature("dog"), new Feature("dog", 2))));

    // test that feature ordering doesn't matter and that extra features are ignored
    Assert.assertArrayEquals(expected, encoder.encodeAll(Arrays.asList(
        new Feature("badger", 42),
        new Feature("dog"),
        new Feature(46))));

    // test that encoder can be serialized and deserialized and give the same results
    byte[] encoderBytes = SerializationUtils.serialize(encoder);
    encoder = (FeatureNodeArrayEncoder) SerializationUtils.deserialize(encoderBytes);
    Assert.assertArrayEquals(expected, encoder.encodeAll(Arrays.asList(
        new Feature("badger", 42),
        new Feature("dog"),
        new Feature(46))));
  }
}
