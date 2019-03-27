package com.kazurayam.materials.impl

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.kazurayam.materials.FileType
import com.kazurayam.materials.Helpers
import com.kazurayam.materials.TSuiteName
import com.kazurayam.materials.TSuiteTimestamp
import com.kazurayam.materials.model.MaterialFileName
import com.kazurayam.materials.model.Suffix

import spock.lang.IgnoreRest
import spock.lang.Specification

//@Ignore
class MaterialRepositoryImplSpec extends Specification {

    // fields
    static Logger logger_ = LoggerFactory.getLogger(MaterialRepositoryImplSpec.class)

    private static Path workdir_
    private static Path fixture_ = Paths.get("./src/test/fixture")
    private static Path materials_ = fixture_.resolve('Materials')
    private static String classShortName_ = Helpers.getClassShortName(MaterialRepositoryImplSpec.class)

    // fixture methods
    def setupSpec() {
        workdir_ = Paths.get("./build/tmp/testOutput/${classShortName_}")
        if (!workdir_.toFile().exists()) {
            workdir_.toFile().mkdirs()
        }
    }
    def setup() {}
    def cleanup() {}
    def cleanupSpec() {}

    // feature methods
    def testGetBaseDir() {
        setup:
        Path casedir = workdir_.resolve('testGetBaseDir')
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        when:
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        then:
        mri.getBaseDir() == materialsDir
    }

    def testResolveScreenshotPath() {
        setup:
        Path casedir = workdir_.resolve('testResolveScreenshotPath')
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        mri.putCurrentTestSuite('TS1', '20180530_130604')
        when:
        Path p1 = mri.resolveScreenshotPath('TC1', Paths.get('.'),
            new URL('https://my.home.net/gn/issueList.html?corp=abcd'))
        then:
        p1.getFileName().toString() == 'https%3A%2F%2Fmy.home.net%2Fgn%2FissueList.html%3Fcorp%3Dabcd.png'
        when:
        Path p2 = mri.resolveScreenshotPath('TC1', Paths.get('.'),
            new URL('https://foo:bar@dev.home.net/gnc/issueList.html?corp=abcd'))
        then:
        p2.getFileName().toString() == 'https%3A%2F%2Ffoo%3Abar%40dev.home.net%2Fgnc%2FissueList.html%3Fcorp%3Dabcd.png'   
    }
    
    
    def testURL() {
        setup:
        URL url = new URL('https://my.home.net/gn/issueList.html?corp=abcd&foo=bar#top')
        expect:
        url.getPath() == '/gn/issueList.html'
        when:
        Path p = Paths.get(url.getPath())
        then:
        p.getNameCount() == 2
        p.getName(0).toString() == 'gn'
        p.getName(1).toString() == 'issueList.html'
        expect:
        url.getQuery() == 'corp=abcd&foo=bar'
        url.getRef() == 'top'
        when:
        Map<String, String> queries = MaterialRepositoryImpl.parseQuery(url.getQuery())
        then:
        queries.size() == 2
        queries.containsKey('corp')
        queries.containsKey('foo')
        queries.get('corp') == 'abcd'
        queries.get('foo') == 'bar'
        when:
        url = new URL('https://www.google.com')
        then:
        url.getPath() == ''
    }
    
    def testParseQuery() {
        setup:
        String query = 'corp=abcd&foo=bar'
        when:
        Map<String, String> queries = MaterialRepositoryImpl.parseQuery(query)
        then:
        queries.size() == 2
        queries.containsKey('corp')
        queries.containsKey('foo')
        queries.get('corp') == 'abcd'
        queries.get('foo') == 'bar'    
    }
    
    
    def testResolveScreenshotPathByURLPathComponents() {
        setup:
        Path casedir = workdir_.resolve('testResolveScreenshotPathByURLPathComponents')
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        mri.putCurrentTestSuite('TS1', '20180530_130604')
        when:
        Path p = mri.resolveScreenshotPathByURLPathComponents('TC1', Paths.get('.'),
                        new URL('https://my.home.net/gn/issueList.html?corp=abcd'))
        then:
        p.getName(p.getNameCount() - 1).toString() == 'gn%2FissueList.html%3Fcorp%3Dabcd.png'
        p.getFileName().toString() == 'gn%2FissueList.html%3Fcorp%3Dabcd.png'
        //
        when:
        Path p0 = mri.resolveScreenshotPathByURLPathComponents('TC1', Paths.get('.'),
                        new URL('https://my.home.net/gn/issueList.html?corp=abcd'), 0)
        then:
        p0.getName(p0.getNameCount() - 1).toString() == 'gn%2FissueList.html%3Fcorp%3Dabcd.png'
        p0.getFileName().toString() == 'gn%2FissueList.html%3Fcorp%3Dabcd.png'
        //
        when:
        Path p1 = mri.resolveScreenshotPathByURLPathComponents('TC1', Paths.get('.'),
                        new URL('https://my.home.net/gn/issueList.html?corp=abcd'), 1)
        then:
        p1.getFileName().toString() == 'issueList.html%3Fcorp%3Dabcd.png'
        when:
        Path p2 = mri.resolveScreenshotPathByURLPathComponents('TC1', Paths.get('.'),
                        new URL('https://my.home.net/gn/issueList.html?corp=abcd'), 2)
        then:
        p2.getFileName().toString() == 'https%3A%2F%2Fmy.home.net%2Fgn%2FissueList.html%3Fcorp%3Dabcd.png'
        //
        when:
        Path google = mri.resolveScreenshotPathByURLPathComponents('TC1', Paths.get('.'),
            new URL('https://www.google.com'))
        then:
        google.getFileName().toString() == 'https%3A%2F%2Fwww.google.com.png'
    }
    
    def testResolveMaterialPath() {
        setup:
        def methodName ='testResolveMaterialPath'
        Path casedir = workdir_.resolve(methodName)
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        mri.putCurrentTestSuite('TS1', '20180530_130604')
        when:
        String materialFileName = MaterialFileName.format(
            new URL('http://demoaut.katalon.com/'),
            Suffix.NULL,
            FileType.PNG)
        Path p = mri.resolveMaterialPath('TC1', materialFileName)
        then:
        p != null
        p.toString().replace('\\', '/') ==
            "build/tmp/testOutput/${classShortName_}/${methodName}/Materials/TS1/20180530_130604/TC1/http%3A%2F%2Fdemoaut.katalon.com%2F.png"
    }

    def testResolveMaterialPath_withSuffix() {
        setup:
        def methodName = 'testResolveMaterialPath_withSuffix'
        Path casedir = workdir_.resolve(methodName)
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        mri.putCurrentTestSuite('TS1', '20180530_130604')
        when:
        String materialFileName = MaterialFileName.format(
            new URL('http://demoaut.katalon.com/'),
            new Suffix(1),
            FileType.PNG)
        Path p = mri.resolveMaterialPath('TC1', materialFileName)
        then:
        p != null
        p.toString().replace('\\', '/') ==
            "build/tmp/testOutput/${classShortName_}/${methodName}/Materials/TS1/20180530_130604/TC1/http%3A%2F%2Fdemoaut.katalon.com%2F(1).png"
    }

    def testResolveMaterialPath_new() {
        setup:
        def methodName = 'testResolveMaterialPath_new'
        Path casedir = workdir_.resolve(methodName)
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        mri.putCurrentTestSuite('TS3', '20180614_152000')
        when:
        String materialFileName = MaterialFileName.format(new URL('http://demoaut.katalon.com/'),
            Suffix.NULL,
            FileType.PNG)
        Path p = mri.resolveMaterialPath('TC1', materialFileName)
        then:
        p != null
        p.toString().replace('\\', '/') ==
            "build/tmp/testOutput/${classShortName_}/${methodName}/Materials/TS3/20180614_152000/TC1/http%3A%2F%2Fdemoaut.katalon.com%2F.png"
        Files.exists(p.getParent())
    }

    def testResolveMaterialPath_withSuffix_new() {
        setup:
        def methodName = 'testResolveMaterialPath_withSuffix_new'
        Path casedir = workdir_.resolve(methodName)
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        mri.putCurrentTestSuite('TS3', '20180614_152000')
        when:
        String materialFileName = MaterialFileName.format(new URL('http://demoaut.katalon.com/'),
            new Suffix(1),
            FileType.PNG)
        Path p = mri.resolveMaterialPath('TC1', materialFileName)
        then:
        p != null
        p.toString().replace('\\', '/') ==
            "build/tmp/testOutput/${classShortName_}/${methodName}/Materials/TS3/20180614_152000/TC1/http%3A%2F%2Fdemoaut.katalon.com%2F(1).png"
        Files.exists(p.getParent())
    }

    def testResolveMaterial_png_SuitelessTimeless() {
        setup:
        def methodName = 'testResolveMaterial_png_SuitelessTimeless'
        Path casedir = workdir_.resolve(methodName)
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        mri.putCurrentTestSuite(TSuiteName.SUITELESS, TSuiteTimestamp.TIMELESS)
        when:
        String materialFileName = MaterialFileName.format(new URL('http://demoaut.katalon.com/'), new Suffix(1), FileType.PNG)
        Path p = mri.resolveMaterialPath('TC1', materialFileName)
        then:
        p != null
        p.toString().replace('\\', '/') == "build/tmp/testOutput/${classShortName_}/${methodName}/Materials/_/_/TC1/http%3A%2F%2Fdemoaut.katalon.com%2F(1).png"
    }

    def testToJsonText() {
        setup:
        Path casedir = workdir_.resolve('testToJsonText')
        Helpers.copyDirectory(fixture_, casedir)
        Path materialsDir = casedir.resolve('Materials')
        Path reportsDir   = casedir.resolve('Reports')
        MaterialRepositoryImpl mri = MaterialRepositoryImpl.newInstance(materialsDir, reportsDir)
        mri.putCurrentTestSuite('TS1')
        when:
        def str = mri.toJsonText()
        then:
        str != null
        str.contains('{"MaterialRepository":{')
        str.contains(Helpers.escapeAsJsonText(casedir.toString()))
        str.contains('}}')
    }

}