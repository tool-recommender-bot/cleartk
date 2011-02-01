/** 
 * Copyright (c) 2011, Regents of the University of Colorado 
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
package org.cleartk.classifier.libsvm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.IOUtils;
import org.cleartk.classifier.Classifier;
import org.cleartk.classifier.jar.ClassifierBuilder_ImplBase;
import org.cleartk.classifier.jar.JarStreams;
import org.cleartk.classifier.util.featurevector.FeatureVector;

/**
 * <br>
 * Copyright (c) 2011, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Steven Bethard
 */
public abstract class GenericLIBSVMClassifierBuilder<CLASSIFIER_TYPE extends Classifier<OUTCOME_TYPE>, OUTCOME_TYPE, ENCODED_OUTCOME_TYPE, MODEL_TYPE>
    extends
    ClassifierBuilder_ImplBase<CLASSIFIER_TYPE, FeatureVector, OUTCOME_TYPE, ENCODED_OUTCOME_TYPE> {

  @Override
  public File getTrainingDataFile(File dir) {
    return new File(dir, "training-data.libsvm");
  }

  public File getModelFile(File dir) {
    return new File(dir, this.getModelName());
  }

  protected abstract String getCommand();

  protected abstract String getModelName();

  protected abstract MODEL_TYPE loadModel(InputStream inputStream) throws IOException;

  public static final String ATTRIBUTES_NAME = "LIBSVM";

  public GenericLIBSVMClassifierBuilder() {
    super();

    // set manifest attributes for classifier
    // TODO: Are these still needed? What code uses them?
    Map<String, Attributes> entries = this.manifest.getEntries();
    if (!entries.containsKey(ATTRIBUTES_NAME)) {
      entries.put(ATTRIBUTES_NAME, new Attributes());
    }
    Attributes attributes = entries.get(ATTRIBUTES_NAME);
    attributes.putValue("scaleFeatures", "normalizeL2");
  }

  @Override
  public void trainClassifier(File dir, String... args) throws Exception {
    String[] command = new String[args.length + 3];
    command[0] = this.getCommand();
    System.arraycopy(args, 0, command, 1, args.length);
    command[command.length - 2] = new File(dir, "training-data.libsvm").getPath();
    command[command.length - 1] = new File(dir, this.getModelName()).getPath();
    Process process = Runtime.getRuntime().exec(command);
    IOUtils.copy(process.getInputStream(), System.out);
    IOUtils.copy(process.getErrorStream(), System.err);
    process.waitFor();
  }

  @Override
  protected void packageClassifier(File dir, JarOutputStream modelStream) throws IOException {
    super.packageClassifier(dir, modelStream);
    JarStreams.putNextJarEntry(modelStream, this.getModelName(), this.getModelFile(dir));
  }

  protected MODEL_TYPE model;

  @Override
  protected void unpackageClassifier(JarInputStream modelStream) throws IOException {
    super.unpackageClassifier(modelStream);
    JarStreams.getNextJarEntry(modelStream, this.getModelName());
    this.model = this.loadModel(modelStream);
  }
}