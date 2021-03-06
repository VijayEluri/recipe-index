package org.blackbeanbag.recipe;


import org.apache.log4j.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import org.blackbeanbag.recipe.scanners.Scanner;
import org.blackbeanbag.recipe.scanners.TextScanner;
import org.blackbeanbag.recipe.scanners.WordScanner;

import java.io.File;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The Indexer class creates a Lucene index for supported documents.
 * New document types are supported via the {@link Scanner} interface
 * and can be provided via the Indexer constructor.
 */
public class Indexer {
    /**
     * Logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(Indexer.class);

    /**
     * Directory containing documents to index.
     */
    private String m_docDir;

    /**
     * Directory containing the index.
     */
    private String m_indexDir;

    /**
     * The Lucene index writer.
     */
    private IndexWriter m_writer;

    /**
     * A collection of supported document scanners.
     */
    private Collection<Scanner> m_scanners;

    /**
     * Construct an Indexer that supports Word and text files.
     *
     * @param docDir    directory containing documents to index
     * @param indexDir  directory containing the index
     */
    public Indexer(String docDir, String indexDir) {
        this(docDir, indexDir, Arrays.asList(new WordScanner(), new TextScanner()));
    }

    /**
     * Construct an Indexer. This constructor requires a non null collection
     * of {@link Scanner}s in order to support scanning of documents.
     *
     * @param docDir    directory containing documents to index
     * @param indexDir  directory containing the index
     * @param scanners  a collection of {@link Scanner} objects
     */
    public Indexer(String docDir, String indexDir, Collection<Scanner> scanners) {
        this.m_docDir = docDir;
        this.m_indexDir = indexDir;
        this.m_scanners = scanners;

        try {
            Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_47);
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
            Directory directory = FSDirectory.open(new File(indexDir));
            this.m_writer = new IndexWriter(directory, config);
            this.m_writer.deleteAll();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the directory containing documents to index.
     *
     * @return directory containing documents to index
     */
    public String getDocDir() {
        return m_docDir;
    }

    /**
     * Return the directory containing the index file.
     *
     * @return the directory containing the index
     */
    public String getIndexDir() {
        return m_indexDir;
    }

    /**
     * Return the Lucene index writer.
     *
     * @return the index writer
     */
    public IndexWriter getWriter() {
        return m_writer;
    }

    /**
     * Return the collection of scanners used by this indexer.
     *
     * @return scanners used by this indexer
     */
    public Collection<Scanner> getScanners() {
        return m_scanners;
    }

    /**
     * Create the index.
     */
    public void createIndex() {
        LOG.debug("Creating index");

        scanDirectory();

        try {
            getWriter().close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Close the indexer.
     */
    public void close() {
        try {
            m_writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Scan the configured document directory for supported
     * documents.
     */
    protected void scanDirectory() {
        scanDirectory(new File(getDocDir()));
    }

    /**
     * Scan the given directory file for supported documents.
     *
     * @param fileDir directory to search
     */
    protected void scanDirectory(File fileDir) {
        List<File> dirList = new LinkedList<File>();

        if (!fileDir.isDirectory()) {
            throw new IllegalArgumentException(fileDir + " must be a directory");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning directory " + fileDir);
        }

        File[] files = fileDir.listFiles();
        if (files == null) {
            throw new IllegalArgumentException(fileDir + " does not contain any files");
        }
        for (File file : files) {
            if (file.isDirectory()) {
                dirList.add(file);
            }
            else {
                String fileName = file.getAbsolutePath();

                for (Scanner scanner : getScanners()) {
                    if (scanner.supportsFile(fileName)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Scanning file " + fileName);
                        }
                        try {
                            getWriter().addDocument(scanner.scan(fileName));
                        } catch (Exception e) {
                            LOG.warn("Could not process file " + fileName, e);
                        }
                    }
                }
            }
        }

        for (File dir : dirList) {
            scanDirectory(dir);
        }
    }
}
