/** 
 * Copyright (c) 2007-2008, Regents of the University of Colorado 
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
package org.cleartk.classifier.encoder.features;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

import org.cleartk.classifier.Feature;
import org.cleartk.classifier.encoder.CleartkEncoderException;
import org.cleartk.classifier.feature.TypePathFeature;
import org.cleartk.classifier.feature.WindowFeature;
import org.cleartk.test.DefaultTestBase;
import org.junit.Test;

/**
 * <br>
 * Copyright (c) 2007-2008, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @deprecated This test class is deprecated to remove compiler warnings related to the use of the
 *             now deprecated WindowFeature and WindowNgramFeature classes which are now deprecated.
 *             Because the NameNumber classes are planned to be deprecated also it does not make
 *             sense to update these tests (see Issue 279)
 */
@Deprecated
public class NameNumberFeatureEncoderTest extends DefaultTestBase {

  @Test
  public void testEncodeCompress() throws Throwable {

    NameNumberFeaturesEncoder nnfe = getDefaultEncoder(true, true);

    testNN("0", 1.0f, new Feature("Hello"), nnfe);

    testNN("1", 1.0f, new Feature("Goodbye"), nnfe);

    testNN("1", 1.0f, new Feature("Goodbye"), nnfe);

    testNN("2", 1.0f, new Feature("Please"), nnfe);

    testNN("3", 1.0f, new Feature("don't"), nnfe);

    testNN("4", 1.0f, new Feature("go"), nnfe);

    testNN("5", 15.0f, new Feature("!", 15), nnfe);

    nnfe.finalizeFeatureSet(outputDirectory);
    File featureMapFile = new File(outputDirectory, NameNumberFeaturesEncoder.LOOKUP_FILE_NAME);

    BufferedReader reader = new BufferedReader(new FileReader(featureMapFile));
    assertEquals("6", reader.readLine());
    assertEquals("!\t5", reader.readLine());
    assertEquals("Goodbye\t1", reader.readLine());
    assertEquals("Hello\t0", reader.readLine());
    assertEquals("Please\t2", reader.readLine());
    assertEquals("don't\t3", reader.readLine());
    assertEquals("go\t4", reader.readLine());
    reader.close();

  }

  @Test
  public void testOnTypePathFeatures() throws CleartkEncoderException {
    NameNumberFeaturesEncoder nnfe = getDefaultEncoder(false, false);

    testNN("_hello", 1.0f, new Feature("", "hello"), nnfe);
    testNN("hello_", 1.0f, new Feature("hello", ""), nnfe);
    testNN("3.1415f_", 1.0f, new Feature("3.1415f", ""), nnfe);
    testNN("_3.1415f", 1.0f, new Feature("", "3.1415f"), nnfe);

    testNN("hello%U003Dgoodbye_two%U003D2", 1.0f, new Feature("hello=goodbye", "two=2"), nnfe);

    // test nominal features
    testNN("hello_goodbye", 1.0f, new Feature("hello", "goodbye"), nnfe);
    testNN("hello(PathToTypeFeature)_goodbye", 1.0f, new TypePathFeature(
        "hello",
        "goodbye",
        "path/to/type/feature"), nnfe);

    // test numeric features
    testNN("hello", 4.0f, new Feature("hello", 4), nnfe);
    testNN("hello", 42.0f, new Feature("hello", 42l), nnfe);
    testNN("hello", 5.0f, new Feature("hello", 5f), nnfe);
    testNN("hello", 6.0f, new Feature("hello", 6d), nnfe);
    testNN("hello", 1.234567f, new Feature("hello", 1.234567d), nnfe);
    testNN("hello(PathToTypeFeature)", 15.0f, new TypePathFeature(
        "hello",
        15.0d,
        "path/to/type/feature"), nnfe);

    testNN("hello_MR3OOB2", 0.0020f, new WindowFeature(
        "hello",
        0.002d,
        WindowFeature.ORIENTATION_MIDDLE_REVERSE,
        3,
        null,
        2), nnfe);

  }

  /**
   * @deprecated This test is deprecated to remove compiler warnings related to the use of the now
   *             deprecated WindowFeature and WindowNgramFeature classes which are now deprecated.
   *             Because the NameNumber classes are planned to be deprecated also it doesn not make
   *             sense to update these tests (see Issue 279)
   */
  @Deprecated
  @Test
  public void testOnWindowFeatures() throws CleartkEncoderException {
    NameNumberFeaturesEncoder nnfe = getDefaultEncoder(false, false);

    testNN("Ccccccc_aaaaaa", 1.0f, new Feature("Ccccccc", "aaaaaa"), nnfe);

    Feature pathFeature = new TypePathFeature("bbbbbbb", "aaaaaa", "black/belt/in/karate");
    testNN("bbbbbbb(BlackBeltInKarate)_aaaaaa", 1.0f, pathFeature, nnfe);
    pathFeature = new TypePathFeature(null, null, "black/belt/in/karate");
    testNN("TypePath(BlackBeltInKarate)", 1.0f, pathFeature, nnfe);

    pathFeature = new TypePathFeature(null, "aaaaaa", "black/belt/in/karate");
    testNN("TypePath(BlackBeltInKarate)_aaaaaa", 1.0f, pathFeature, nnfe);

    testNN("Ccccccc_L0_TypePath(BlackBeltInKarate)_aaaaaa", 1.0f, new WindowFeature(
        "Ccccccc",
        "aaaaaa",
        WindowFeature.ORIENTATION_LEFT,
        0,
        pathFeature,
        0), nnfe);
    testNN("Window_L0_TypePath(BlackBeltInKarate)_aaaaaa", 1.0f, new WindowFeature(
        null,
        "aaaaaa",
        WindowFeature.ORIENTATION_LEFT,
        0,
        pathFeature,
        0), nnfe);
    testNN("Ccccccc_L0_aaaaaa", 1.0f, new WindowFeature(
        "Ccccccc",
        "aaaaaa",
        WindowFeature.ORIENTATION_LEFT,
        0,
        null,
        0), nnfe);
    testNN("Ccccccc_MR3OOB2_aaaaaa", 1.0f, new WindowFeature(
        "Ccccccc",
        "aaaaaa",
        WindowFeature.ORIENTATION_MIDDLE_REVERSE,
        3,
        null,
        2), nnfe);
    testNN("Window_MR3OOB2", 1.0f, new WindowFeature(
        null,
        null,
        WindowFeature.ORIENTATION_MIDDLE_REVERSE,
        3,
        null,
        2), nnfe);
    testNN("Window_MR3OOB2", 123456.0f, new WindowFeature(
        null,
        Integer.valueOf(123456),
        WindowFeature.ORIENTATION_MIDDLE_REVERSE,
        3,
        null,
        2), nnfe);

    Feature typePathFeature = new TypePathFeature(null, null, "pos");
    testNN("Window_L2OOB2_TypePath(Pos)", 1.0f, new WindowFeature(
        null,
        null,
        WindowFeature.ORIENTATION_LEFT,
        2,
        typePathFeature,
        2), nnfe);
    testNN("Window_L2_TypePath(Pos)", 1.0f, new WindowFeature(
        null,
        null,
        WindowFeature.ORIENTATION_LEFT,
        2,
        typePathFeature), nnfe);
    testNN("Window_L2", 1.0f, new WindowFeature(
        null,
        null,
        WindowFeature.ORIENTATION_LEFT,
        2,
        null,
        0), nnfe);

  }

  private void testNN(String name, Number number, Feature feature, NameNumberFeaturesEncoder nnfe)
      throws CleartkEncoderException {
    List<NameNumber> cvs = nnfe.encodeAll(Arrays.asList(feature));
    if (cvs == null || cvs.size() == 0)
      return;
    NameNumber nameNumber = cvs.get(0);
    assertEquals(name, nameNumber.name);
    assertEquals(number.floatValue(), nameNumber.number.floatValue(), 0.01d);
  }

  private NameNumberFeaturesEncoder getDefaultEncoder(boolean compress, boolean sort) {
    NameNumberFeaturesEncoder featuresEncoder = new NameNumberFeaturesEncoder(compress, sort);
    featuresEncoder.addEncoder(new NumberEncoder());
    featuresEncoder.addEncoder(new BooleanEncoder());
    featuresEncoder.addEncoder(new StringEncoder());
    return featuresEncoder;
  }
}