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
package org.sral.nifi.processors.pdf2tiff;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfToMultiPageTIFFConverter {

    public void convert(InputStream inputStream,  OutputStream outputStream, int renderDpi, File tmpPath) throws IOException {

        PDDocument document = PDDocument.load(inputStream);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
        ImageWriter imageWriter = writers.next();

        ImageOutputStream imageOutputStream = new FileCacheImageOutputStream(outputStream, tmpPath);

        imageWriter.setOutput(imageOutputStream);
        imageWriter.prepareWriteSequence(null);

        PDFRenderer renderer = new PDFRenderer(document);
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, renderDpi);
            ImageWriteParam param = imageWriter.getDefaultWriteParam();
            IIOMetadata metadata = imageWriter.getDefaultImageMetadata(new ImageTypeSpecifier(image), param);
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            TIFFUtil.setCompressionType(param, image);
            TIFFUtil.updateMetadata(metadata, image, renderDpi);
            imageWriter.writeToSequence(new IIOImage(image, null, metadata), param);
        }

        imageWriter.endWriteSequence();
        imageWriter.dispose();
        imageOutputStream.flush();
        imageOutputStream.close();
    }
}
