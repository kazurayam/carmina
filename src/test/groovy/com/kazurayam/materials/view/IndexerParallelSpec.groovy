package com.kazurayam.materials.view

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.kazurayam.materials.Helpers
import com.kazurayam.materials.Indexer
import com.kazurayam.materials.Material
import com.kazurayam.materials.MaterialPairs
import com.kazurayam.materials.MaterialRepository
import com.kazurayam.materials.MaterialRepositoryFactory
import com.kazurayam.materials.MaterialStorage
import com.kazurayam.materials.MaterialStorageFactory
import com.kazurayam.materials.TCaseName
import com.kazurayam.materials.TCaseResult
import com.kazurayam.materials.TSuiteName
import com.kazurayam.materials.TSuiteResult
import com.kazurayam.materials.TSuiteResultId
import com.kazurayam.materials.TSuiteTimestamp
import com.kazurayam.materials.imagedifference.ImageCollectionDiffer
import com.kazurayam.materials.impl.TSuiteResultIdImpl
import com.kazurayam.materials.stats.ImageDeltaStats
import com.kazurayam.materials.stats.StorageScanner

import spock.lang.Specification

class IndexerParallelSpec extends Specification {
    
    static Logger logger_ = LoggerFactory.getLogger(IndexerParallelSpec.class)
    
    // fields
    static Path specOutputDir
    static Path fixtureDir
    
    // fixture methods
    def setupSpec() {
        Path projectDir = Paths.get('.')
        Path testOutputDir = projectDir.resolve('./build/tmp/testOutput')
        specOutputDir = testOutputDir.resolve("${Helpers.getClassShortName(IndexerParallelSpec.class)}")
        //if (specOutputDir.toFile().exists()) {
        //    Helpers.deleteDirectoryContents(specOutputDir)
        //}
        fixtureDir = Paths.get(
            "./src/test/fixtures/com.kazurayam.materials.view.RepositoryVisitorGeneratingHtmlDivsXXXXSpec")
    }
    def setup() {}
    def cleanup() {}
    def cleanupSpec() {}
    
    // feature methods
    def testSmoke() {
        setup:
        Path caseOutputDir = specOutputDir.resolve('testSmoke')
        Files.createDirectories(caseOutputDir)
        Helpers.copyDirectory(fixtureDir, caseOutputDir, true)  // 3rd arg means 'skipIfIdentical'
        Indexer indexer = makeIndexerParallel(caseOutputDir)
        when:
        indexer.execute()
        Path index = indexer.getOutput()
        logger_.debug("#testSmoke index=${index.toString()}")
        then:
        Files.exists(index)
        when:
        String html = index.toFile().text
        then:
        html.contains('<html')
        html.contains('<head')
        html.contains('http-equiv')
        html.contains('<meta charset')
        // html.contains('<!-- [if lt IE 9]')
        html.contains('bootstrap.min.css')
        html.contains('bootstrap-treeview.min.css')
        // html.contains('.list-group-item > .badge {')
        html.contains('<body>')
        html.contains('<div id="tree"')
        html.contains('<div id="footer"')
        
        // div tags as Modal
        html.contains('<div id="modal-windows"')
        html.contains('class="modal fade"')
        //html.contains('<div class="modal-dialog modal-lg')
        html.contains('<div class="modal-dialog modal-xl')
        html.contains('<div class="modal-content"')
        html.contains('<div class="modal-header"')
        html.contains('<p class="modal-title"')
        html.contains('<div class="modal-body"')
        html.contains('<img src="')
        html.contains('<div class="modal-footer"')
        html.contains('class="btn')
        
        
        // script tags
        html.contains('jquery')
        html.contains('popper')
        html.contains('bootstrap')
        html.contains('bootstrap-treeview')
        
        // Bootstrap Treeview data
        html.contains('function getTree() {')
        html.contains('var data = [')
        html.contains('function modalize() {')
        html.contains('$(\'#tree\').treeview({')
        html.contains('modalize();')
    }
        
    /**
     * helper to make a ParallelIndexer object
     * @param caseOutputDir
     * @return a ParallelIndexer object
     */
    private Indexer makeIndexerParallel(Path caseOutputDir) {
        Path materialsDir = caseOutputDir.resolve('Materials')
        Path reportsDir   = caseOutputDir.resolve('Reports')
        Indexer indexer = new IndexerParallel()
        indexer.setBaseDir(materialsDir)
        indexer.setReportsDir(reportsDir)
        Path index = materialsDir.resolve('index.html')
        indexer.setOutput(index)
        return indexer
    }
}
