package com.kazurayam.materials.view

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.kazurayam.materials.Helpers

import spock.lang.Specification

class BaseIndexerSpec extends Specification {
    
    static Logger logger_ = LoggerFactory.getLogger(BaseIndexerSpec.class)
    
    // fields
    static Path specOutputDir
    static Path fixtureDir
    
    // fixture methods
    def setupSpec() {
        Path projectDir = Paths.get('.')
        Path testOutputDir = projectDir.resolve('./build/tmp/testOutput')
        specOutputDir = testOutputDir.resolve("${Helpers.getClassShortName(BaseIndexerSpec.class)}")
        if (specOutputDir.toFile().exists()) {
            Helpers.deleteDirectoryContents(specOutputDir)
        }
        fixtureDir = projectDir.resolve('src').resolve('test').resolve('fixture')
    }
    def setup() {}
    def cleanup() {}
    def cleanupSpec() {}
    
    // feature methods
    def testSmoke() {
        setup:
        Path caseOutputDir = specOutputDir.resolve('testSmoke')
        Files.createDirectories(caseOutputDir)
        Helpers.copyDirectory(fixtureDir, caseOutputDir)
        BaseIndexer indexer = makeIndexer(caseOutputDir)
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
        html.contains('<div id="modal-windows"')
        
        // div tags as Modal
        html.contains('FOOfooFOO BAR')
        
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
     * helper to make a CarouselIndexer object
     * @param caseOutputDir
     * @return a CarouselIndexer object
     */
    private BaseIndexer makeIndexer(Path caseOutputDir) {
        Path materialsDir = caseOutputDir.resolve('Materials')
        Path reportsDir   = caseOutputDir.resolve('Reports')
        BaseIndexer indexer = new BaseIndexer()
        indexer.setBaseDir(materialsDir)
        indexer.setReportsDir(reportsDir)
        Path index = materialsDir.resolve('index.html')
        indexer.setOutput(index)
        return indexer
    }
}
