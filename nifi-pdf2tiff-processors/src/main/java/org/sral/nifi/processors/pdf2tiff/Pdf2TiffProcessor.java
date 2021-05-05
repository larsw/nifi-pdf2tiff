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

import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.Restricted;
import org.apache.nifi.annotation.behavior.Restriction;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.RequiredPermission;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.File;
import java.util.*;

@Restricted(
        restrictions = {
                @Restriction(
                        requiredPermission = RequiredPermission.READ_FILESYSTEM,
                        explanation = "Needed for temporary caching on the file system during rendering."),
                @Restriction(
                        requiredPermission = RequiredPermission.WRITE_FILESYSTEM,
                        explanation = "Needed for temporary caching on the file system during rendering.")
        }
)
@SideEffectFree
@EventDriven
@Tags({"tiff", "pdf", "converter"})
@CapabilityDescription("A processor that converts PDF to multi-page TIFF files.")
//@WritesAttributes({@WritesAttribute(attribute="convertedPages", description="The number of pages converted.")})
public class Pdf2TiffProcessor extends AbstractProcessor {

    public static final PropertyDescriptor RENDER_DPI = new PropertyDescriptor
            .Builder().name("RENDER_DPI")
            .displayName("Render DPI")
            .description("Dots per Inches (DPI) used during rendering.")
            .required(true)
            .defaultValue("150")
            .addValidator(StandardValidators.POSITIVE_LONG_VALIDATOR)
            .build();

    public static final PropertyDescriptor RENDER_TEMP_DIRECTORY = new PropertyDescriptor
            .Builder().name("RENDER_TEMP_DIRECTORY")
            .displayName("Render temporary directory")
            .description("The directory where the rendering process can temporarily store intermediate results.")
            .required(true)
            .defaultValue("/tmp")
            .addValidator(StandardValidators.FILE_EXISTS_VALIDATOR)
            .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("SUCCESS")
            .description("Successfully converted TIFFs.")
            .build();

    public static final Relationship FAILURE = new Relationship.Builder()
            .name("FAILURE")
            .description("PDFs that couldn't be converted.")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(RENDER_DPI);
        descriptors.add(RENDER_TEMP_DIRECTORY);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(SUCCESS);
        relationships.add(FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile pdfFile = session.get();
        if ( pdfFile == null ) {
            return;
        }
        PdfToMultiPageTIFFConverter converter = new PdfToMultiPageTIFFConverter();
        String fileName = pdfFile.getAttribute("filename").replaceAll("\\.[pP][dD][fF]$", ".tiff");
        session.putAttribute(pdfFile, "filename", fileName);
        try {
            int renderDpi = context.getProperty(RENDER_DPI).asInteger();
            String tmpPath = context.getProperty(RENDER_TEMP_DIRECTORY).getValue();
            File tmpPathFile = new File(tmpPath);
            FlowFile tiffFile = session.write(pdfFile, (inputStream, outputStream) -> converter.convert(inputStream, outputStream, renderDpi, tmpPathFile));
            session.transfer(tiffFile, SUCCESS);
            List<FlowFile> outFiles = new ArrayList<>();
            outFiles.add(tiffFile);
            session.getProvenanceReporter().fork(pdfFile, outFiles);
        } catch (ProcessException ex) {
            session.transfer(pdfFile, FAILURE);
        }
    }
}
