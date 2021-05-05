/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sral.nifi.processors.pdf2tiff;

import org.junit.Test;

import java.io.*;

public class PdfToMultiPageTIFFConverterTests {

    @Test
    public void testFoo() throws IOException {
        PdfToMultiPageTIFFConverter converter =  new PdfToMultiPageTIFFConverter();
        FileInputStream fis = new FileInputStream("input.pdf");
        File outputFile = File.createTempFile("xyz", "tiff");
        FileOutputStream fos = new FileOutputStream(outputFile);

        converter.convert(fis, fos, 150, new File("/tmp"));

        fis.close();
        fos.close();
        outputFile.delete();
    }
}
