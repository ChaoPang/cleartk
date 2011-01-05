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
package org.cleartk.syntax.constituent.ptb;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Level;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.cleartk.syntax.constituent.TreebankGoldAnnotator;
import org.cleartk.syntax.constituent.TreebankConstants;
import org.cleartk.util.ViewURIUtil;
import org.uimafit.component.JCasCollectionReader_ImplBase;
import org.uimafit.component.ViewCreatorAnnotator;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.descriptor.SofaCapability;
import org.uimafit.factory.ConfigurationParameterFactory;


/**
/**
 * <br>
 * Copyright (c) 2007-2008, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * 
 * 
 * <p>
 * PennTreebankReader reads in the PennTreebank (PTB) data distributed by the
 * LDC. It simply reads the raw treebank data into a view called "TreebankView".
 * To actually parse the treebank data and post it to the CAS, you will need to
 * use the TreebankGoldAnnotator which does the real work of parsing the
 * treebank format. In general, treebank data can be read in by a
 * PlainTextCollectionReader or some other simple collection reader. This class
 * exists because the PennTreebank has a specific directory structure that
 * corresponds to sections which are often used in specific ways to conduct
 * experiments - e.g. section 02-20 for training and sections 21-24 for testing.
 * This collection reader makes it easy to read in specific sections for later
 * processing.  Only files ending with ".mrg" will be read in.  
 * </p>
 * <p>
 * The acronym WSJ stands for Wall Street Journal which is the source of the
 * articles treebanked by PTB.
 * <p>
 * If you are concerned that your copy of PTB is not working correctly with this collection reader (or TreebankGoldAnnotator)
 * then please see the corresponding unit tests.
 * 
 * @see TreebankGoldAnnotator
 * 
 * @author Philip Ogren, Philipp Wetzler
 */

@SofaCapability(outputSofas= {TreebankConstants.TREEBANK_VIEW, ViewURIUtil.URI})
public class PennTreebankReader extends JCasCollectionReader_ImplBase {
	public static final String PARAM_CORPUS_DIRECTORY_NAME = ConfigurationParameterFactory.createConfigurationParameterName(PennTreebankReader.class, "corpusDirectoryName");
	private static final String CORPUS_DIRECTORY_DESCRIPTION = "Specifies the location of WSJ/PennTreebank treebank files.  " +
			"The directory should contain subdirectories corresponding to the sections (e.g. '00', '01', etc.) " +
			"That is, if a local copy of PennTreebank sits at C:/Data/PTB/wsj/mrg, then the the subdirectory C:/Data/PTB/wsj/mrg/00 should exist. " +
			"There are 24 sections in PTB corresponding to the directories 00, 01, 02, ... 24. ";
	@ConfigurationParameter(
			mandatory = true,
			description = CORPUS_DIRECTORY_DESCRIPTION)
	private String corpusDirectoryName;

	public static final String PARAM_SECTIONS_SPECIFIER = ConfigurationParameterFactory.createConfigurationParameterName(PennTreebankReader.class, "sectionsSpecifier");
	private static final String SECTIONS_DESCRIPTION = "specifies which sections of PTB to read in.  " +
			"The required format for values of this parameter allows for comma-separated section numbers and section ranges, " +
			"for example '02,07-12,16'.";
	@ConfigurationParameter(
			defaultValue = "00-24",
			description = SECTIONS_DESCRIPTION)
	private String sectionsSpecifier;
	
	protected File directory;

	protected LinkedList<File> files;

	protected int numberOfFiles;

	protected ListSpecification sections;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		this.sections = new ListSpecification(sectionsSpecifier);

		this.directory = new File(corpusDirectoryName);
		this.files = new LinkedList<File>();
		collectSections(new File(directory.getPath()), this.files, this.sections);
		Collections.sort(files);
		this.numberOfFiles = files.size();

	}

	/**
	 * This will add all the <tt>.mrg</tt> files in the given WSJ sections to
	 * <em>treebankFiles</em>.
	 * 
	 * @param wsjDirectory
	 *            is the top level of the WSJ part of Treebank. Underneath here
	 *            are the section subdirectories.
	 * @param treebankFiles
	 * @param wsjSections
	 *            is the set of sections to include.
	 */
	public static void collectSections(File wsjDirectory, List<File> treebankFiles, ListSpecification wsjSections) {
		if (!wsjDirectory.isDirectory()) return;

		for (File subFile : wsjDirectory.listFiles()) {
			if (!subFile.isDirectory()) continue;

			try {
				int section = Integer.valueOf(subFile.getName());

				if (!wsjSections.contains(section)) continue;
			}
			catch (NumberFormatException e) {
				continue;
			}

			collectFiles(subFile, treebankFiles);
		}
	}

	static void collectFiles(File file, List<File> treebankFiles) {
		if (file.isFile() && file.getName().endsWith(".mrg")) {
			treebankFiles.add(file);
		}
		else if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				collectFiles(subFile, treebankFiles);
			}
		}
	}

	/**
	 * Reads the next file and stores its text in <b>cas</b> as the
	 * "TreebankView" SOFA.
	 * 
	 * @param cas
	 * 
	 * @throws IOException
	 * @throws CollectionException
	 */
	public void getNext(JCas jCas) throws IOException, CollectionException {
		File treebankFile = files.removeFirst();
		getUimaContext().getLogger().log(Level.FINEST, "reading treebank file: " + treebankFile.getPath());
		ViewURIUtil.setURI(jCas, treebankFile.toURI().toString());
		try {
			JCas treebankView = ViewCreatorAnnotator.createViewSafely(jCas, TreebankConstants.TREEBANK_VIEW);
			treebankView.setSofaDataString(FileUtils.file2String(treebankFile), "text/plain");
		} catch(AnalysisEngineProcessException aepe) {
			throw new CollectionException(aepe);
		}
	}

	public void close() throws IOException {
	}

	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(numberOfFiles - files.size(), numberOfFiles, Progress.ENTITIES) };
	}

	public boolean hasNext() throws IOException, CollectionException {
		if (files.size() > 0) return true;
		else return false;
	}

	public void setCorpusDirectoryName(String corpusDirectoryName) {
		this.corpusDirectoryName = corpusDirectoryName;
	}

	public void setSectionsSpecifier(String sectionsString) {
		this.sectionsSpecifier = sectionsString;
	}


}